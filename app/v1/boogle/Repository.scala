package v1.boogle

import javax.inject.{Inject, Singleton}
import akka.actor.ActorSystem
import play.api.libs.concurrent.CustomExecutionContext
import play.api.{Logger, MarkerContext}

import scala.concurrent.Future

final case class BookData(id: String, title: String, body: String)

class BoogleExecutionContext @Inject()(actorSystem: ActorSystem) extends CustomExecutionContext(actorSystem, "repository.dispatcher")

/**
  * A pure non-blocking interface for the Repository.
  */
trait Repository {
  def create(data: BookData)(implicit mc: MarkerContext): Future[String]

  def list()(implicit mc: MarkerContext): Future[Iterable[BookData]]

  def get(id: String)(implicit mc: MarkerContext): Future[Option[BookData]]
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

  // TODO: move these where they're used
  import com.sksamuel.elastic4s.embedded.LocalNode
  import com.sksamuel.elastic4s.http.ElasticDsl._

  private val logger = Logger(this.getClass)

  // TODO: replace Elasticsearch setup with production cluster
  val localNode = LocalNode("mycluster", "/tmp/datapath")
  val client = localNode.client(shutdownNodeOnClose = true)

  // TODO: remove after debug
  client.execute {
    createIndex("boogle").mappings(
      mapping("book").fields(
        textField("title"),
        textField("body")
      )
    )
  }.await

  // TODO: see if we can get rid of the 'await' inside the futures to make everything propery async
  override def list()(implicit mc: MarkerContext): Future[Iterable[BookData]] = {
    Future {
      logger.trace(s"list: ")
      val resp = client.execute {
        search("boogle")
      }.await
      resp.result.hits.hits.map(hit => BookData(hit.id, hit.sourceField("title").toString, hit.sourceField("body").toString))
    }
  }

  override def get(id: String)(implicit mc: MarkerContext): Future[Option[BookData]] = {
    // TODO: refactor to search by ID
    Future {
      logger.trace(s"list: ")
      val resp = client.execute {
        search("boogle")
      }.await
      val matchedHits = resp.result.hits.hits
        .filter(hit => hit.id == id)
        .map(hit => BookData(hit.id, hit.sourceField("title").toString, hit.sourceField("body").toString))

      if (matchedHits.size != 1) None
      else Option(matchedHits.head)
    }
  }

  def create(data: BookData)(implicit mc: MarkerContext): Future[String] = {
    Future {
      logger.trace(s"create: data = $data")
      val resp = client.execute {
        indexInto("boogle" / "book")
          .fields("title" -> data.title, "body" -> data.body)
      }.await
      resp.result.id
    }
  }

}
