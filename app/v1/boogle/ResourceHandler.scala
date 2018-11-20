package v1.boogle

import javax.inject.{Inject, Provider}

import play.api.MarkerContext

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._

/**
  * DTOs
  */
case class BookResource(id: String, title: String, author: String, pages: Map[Int, String])
case class PageResource(id: String, bookId: String, number: String, content: String)

object BookResource {

  /**
    * Mapping to write a BookResource out as a JSON value.
    */
  implicit val implicitWrites = new Writes[BookResource] {
    def writes(resource: BookResource): JsValue = {
      val pages = if (resource.pages != null && resource.pages.size > 0) resource.pages else Map()
      Json.obj(
        "id" -> resource.id,
        "title" -> resource.title,
        "author" -> resource.author,
        "pages" -> pages
      )
    }
  }
}

object PageResource {

  /**
    * Mapping to write a PageResource out as a JSON value.
    */
  implicit val implicitWrites = new Writes[PageResource] {
    def writes(resource: PageResource): JsValue = {
      Json.obj(
        "id" -> resource.id,
        "bookId" -> resource.bookId,
        "number" -> resource.number,
        "content" -> resource.content
      )
    }
  }
}

/**
  * Controls access to the backend Book and Page data
  */
class ResourceHandler @Inject()(routerProvider: Provider[Router],
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

  def lookup(searchPhrase: String)(implicit mc: MarkerContext): Future[Option[PageResource]] = {
    val future = repository.getPageBySearchPhrase(searchPhrase)
    future.map { maybeData =>
      maybeData.map { data =>
        createPageResource(data)
      }
    }
  }

  private def createBookResource(data: BookData): BookResource = {
    BookResource(data.id, data.title, data.author, data.pages)
  }

  private def createPageResource(data: PageData): PageResource = {
    PageResource(data.id, data.bookId, data.number, data.content)
  }

  // TODO: don't need this, just keep pages as an array
  private def createPageMap(pages: List[String]): Map[Int, String] = {
    var pageMap: Map[Int, String] = Map()
    (0 to (pages.size - 1)).foreach(i => pageMap += (i -> pages(i)))
    pageMap
  }

}
