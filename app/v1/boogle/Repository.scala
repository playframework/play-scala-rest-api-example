package v1.boogle

import javax.inject.{Inject, Singleton}
import akka.actor.ActorSystem
import play.api.libs.concurrent.CustomExecutionContext
import play.api.{Logger, MarkerContext}

import scala.concurrent.Future

final case class BookData(id: String, title: String, author: String, pages: Map[Int, String])

class BoogleExecutionContext @Inject()(actorSystem: ActorSystem) extends CustomExecutionContext(actorSystem, "repository.dispatcher")

/**
  * A pure non-blocking interface for the Repository.
  */
trait Repository {
  def indexBook(data: BookData)(implicit mc: MarkerContext): Future[String]

  def listBooks()(implicit mc: MarkerContext): Future[Iterable[BookData]]

  def getBookById(id: String)(implicit mc: MarkerContext): Future[Option[BookData]]
}

/**
  * An Elasticsearch implementation of the repository.
  *
  * A custom execution context is used here to establish that blocking operations should be
  * executed in a different thread than Play's ExecutionContext, which is used for CPU bound tasks
  * such as rendering.
  */
@Singleton
class RepositoryImpl @Inject()()(implicit ec: BoogleExecutionContext) extends Repository {

  import com.sksamuel.elastic4s.embedded.LocalNode
  import com.sksamuel.elastic4s.http.ElasticDsl._

  private val logger = Logger(this.getClass)

  // TODO: replace Elasticsearch setup with production cluster
  // TODO: also remember to clean out this directory when done developing
  val localNode = LocalNode("mycluster", "/tmp/datapath/3")
  val client = localNode.client(shutdownNodeOnClose = true)

  // TODO: remove after debug
  client.execute {
    createIndex("book").mappings(mapping("bookType").fields(
      textField("title"), textField("author")
    ))
    createIndex("page").mappings(mapping("pageType").fields(
      // TODO: make keyword?
      textField("bookId"), intField("number"), textField("content")
    ))
  }.await

  // TODO: see if we can get rid of the 'await' inside the futures to make everything propery async
  override def listBooks()(implicit mc: MarkerContext): Future[Iterable[BookData]] = {
    Future {
      logger.trace(s"list: ")

      val resp = client.execute {
        search("book")
      }.await
      resp.result.hits.hits.map(hit => BookData(hit.id, hit.sourceField("title").toString, hit.sourceField("author").toString, null))
    }
  }

  override def getBookById(id: String)(implicit mc: MarkerContext): Future[Option[BookData]] = {
    // TODO: refactor to search by ID
    Future {
      logger.trace(s"list: ")

      // Get the book
      val bookResponse = client.execute {
        search("book")
      }.await

      val matchedBooks = bookResponse.result.hits.hits
        .filter(hit => hit.id == id)

      if (matchedBooks.length != 1) None
      else {
        val bookId = matchedBooks.head.id
        val title = matchedBooks.head.sourceField("title").toString
        val author = matchedBooks.head.sourceField("author").toString

        // TODO: make this less horribly inefficient
        // Get the pages
        val pageResponse = client.execute {
          search("page")
        }.await

        var pages: Map[Int, String] = Map()
        pageResponse.result.hits.hits
          .filter(hit => hit.sourceField("bookId").toString == id)
          .foreach(hit => pages += (hit.sourceField("number").toString.toInt -> hit.sourceField("content").toString))

        Option(BookData(bookId, title, author, pages))
      }
    }
  }

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
        var pageResp = client.execute {
          indexInto("page" / "pageType")
            .fields("bookId" -> bookId, "number" -> number, "content" -> page)
        }.await
        // DEBUG
        println(pageResp.result.id)
      }

      bookId
    }
  }

}
