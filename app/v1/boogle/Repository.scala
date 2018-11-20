package v1.boogle

import javax.inject.{Inject, Singleton}
import akka.actor.ActorSystem
import play.api.libs.concurrent.CustomExecutionContext
import play.api.{Logger, MarkerContext}

import scala.concurrent.Future

final case class BookData(id: String, title: String, author: String, pages: Map[Int, String])
final case class PageData(id: String, bookId: String, number:String, content: String)

class BoogleExecutionContext @Inject()(actorSystem: ActorSystem) extends CustomExecutionContext(actorSystem, "repository.dispatcher")

/**
  * A pure non-blocking interface for the Repository.
  */
trait Repository {
  def indexBook(data: BookData)(implicit mc: MarkerContext): Future[String]
  def getPageBySearchPhrase(searchPhrase: String)(implicit mc: MarkerContext): Future[Option[PageData]]
}

/**
  * An Elasticsearch implementation of the repository.
  *
  * A custom execution context is used here to establish that blocking operations should be
  * executed in a different thread than Play's ExecutionContext, which is used for CPU bound tasks
  * such as rendering.
  */
@Singleton
// TODO: check implicit EC injection
class RepositoryImpl @Inject()()(implicit ec: BoogleExecutionContext) extends Repository {
  import com.sksamuel.elastic4s.embedded.LocalNode
  import com.sksamuel.elastic4s.http.ElasticDsl._

  private val logger = Logger(this.getClass)

  // In production, replace Elasticsearch setup actual cluster
  val localNode = LocalNode("mycluster", "/tmp/datapath/5")
  val client = localNode.client(shutdownNodeOnClose = true)

  // TODO: move this into post-constuct init method
  client.execute {
    createIndex("book").mappings(mapping("bookType").fields(
      textField("title"), textField("author")
    ))
    createIndex("page").mappings(mapping("pageType").fields(
      // TODO: make keyword or set up parent-child relationship
      textField("bookId"), intField("number"), textField("content")
    ))
  }.await

  override def getPageBySearchPhrase(searchPhrase: String)(implicit mc: MarkerContext): Future[Option[PageData]] = {
    logger.trace(s"get book by search phrase: $searchPhrase")
    client.execute {
      // TODO: fine-tune fuzziness
      search("page") query fuzzyQuery("content", searchPhrase)
    } map { response =>
      if (response.result.hits.hits.size == 0) None
      else {
        // TODO: we're only returning the first result here which is a bit arbitrary - return the best match
        val page = response.result.hits.hits.head
        Option(PageData(page.id, page.sourceField("bookId").toString, page.sourceField("number").toString, page.sourceField("content").toString))
      }
    }
  }

  // TODO: chain futures better to make this responsive
  def indexBook(data: BookData)(implicit mc: MarkerContext): Future[String] = {
    Future {
      logger.trace(s"create: data = $data")

      // Index the book
      val resp = client.execute {
        indexInto("book" / "bookType")
          .fields("title" -> data.title, "author" -> data.author)
      }.await
      val bookId = resp.result.id

      // Index each page
      for ((number, page) <- data.pages) {
        client.execute {
          indexInto("page" / "pageType")
            .fields("bookId" -> bookId, "number" -> number, "content" -> page)
        }
      }

      bookId
    }
  }

}
