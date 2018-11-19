package v1.boogle

import javax.inject.{Inject, Provider}

import play.api.MarkerContext

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._

/**
  * DTO for displaying book information.
  */
case class BookResource(id: String, link: String, title: String, author: String, pages: Map[Int, String])

object BookResource {

  /**
    * Mapping to write a BookResource out as a JSON value.
    */
  implicit val implicitWrites = new Writes[BookResource] {
    def writes(resource: BookResource): JsValue = {
      val pages = if (resource.pages != null && resource.pages.size > 0) resource.pages else Map()
      Json.obj(
        "id" -> resource.id,
        "link" -> resource.link,
        "title" -> resource.title,
        "author" -> resource.author,
        "pages" -> pages
      )
    }
  }
}

/**
  * Controls access to the backend Book data, returning [[BookResource]]
  */
class BookResourceHandler @Inject()(
                                     routerProvider: Provider[Router],
                                     repository: Repository)(implicit ec: ExecutionContext) {

  def create(bookInput: BookInput)(implicit mc: MarkerContext): Future[BookResource] = {
    // TODO: Fix this mutable mess
    val pageMap = createPageMap(bookInput.pages)
    var data = BookData(null, bookInput.title, bookInput.author, pageMap)
    repository.indexBook(data).map { id =>
      data = BookData(id, bookInput.title, bookInput.author, pageMap)
      createBookResource(data)
    }
  }

  def lookup(id: String)(implicit mc: MarkerContext): Future[Option[BookResource]] = {
    val future = repository.getBookById(id)
    future.map { maybeData =>
      maybeData.map { data =>
        createBookResource(data)
      }
    }
  }

  def find(implicit mc: MarkerContext): Future[Iterable[BookResource]] = {
    repository.listBooks().map { dataList =>
      dataList.map(bookData => createBookResource(bookData))
    }
  }

  private def createBookResource(data: BookData): BookResource = {
    BookResource(data.id.toString, routerProvider.get.link(data.id), data.title, data.author, data.pages)
  }

  private def createPageMap(pages: List[String]): Map[Int, String] = {
    var pageMap: Map[Int, String] = Map()
    (0 to (pages.size - 1)).foreach(i => pageMap += (i -> pages(i)))
    pageMap
  }

}
