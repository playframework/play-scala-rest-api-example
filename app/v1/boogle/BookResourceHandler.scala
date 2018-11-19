package v1.boogle

import javax.inject.{Inject, Provider}

import play.api.MarkerContext

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._

/**
  * DTO for displaying book information.
  */
case class BookResource(id: String, link: String, title: String, body: String)

object BookResource {

  /**
    * Mapping to write a BookResource out as a JSON value.
    */
  implicit val implicitWrites = new Writes[BookResource] {
    def writes(resource: BookResource): JsValue = {
      Json.obj(
        "id" -> resource.id,
        "link" -> resource.link,
        "title" -> resource.title,
        "body" -> resource.body
      )
    }
  }
}

/**
  * Controls access to the backend Book data, returning [[BookResource]]
  */
class BookResourceHandler @Inject()(
                                     routerProvider: Provider[Router],
                                     repository: Repository)(implicit ec: BoogleExecutionContext) {

  def create(bookInput: BookInput)(implicit mc: MarkerContext): Future[BookResource] = {
    // TODO: Fix this mutable mess
    var data = BookData(null, bookInput.title, bookInput.body)
    repository.create(data).map { id =>
      data = BookData(id, bookInput.title, bookInput.body)
      createBookResource(data)
    }
  }

  def lookup(id: String)(implicit mc: MarkerContext): Future[Option[BookResource]] = {
    val future = repository.get(id)
    future.map { maybeData =>
      maybeData.map { data =>
        createBookResource(data)
      }
    }
  }

  def find(implicit mc: MarkerContext): Future[Iterable[BookResource]] = {
    repository.list().map { dataList =>
      dataList.map(bookData => createBookResource(bookData))
    }
  }

  private def createBookResource(data: BookData): BookResource = {
    BookResource(data.id.toString, routerProvider.get.link(data.id), data.title, data.body)
  }

}
