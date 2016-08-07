package post

import javax.inject.Inject

import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

case class PostFormInput(title: String, body: String)

/**
 * Takes HTTP requests and produces JSON or HTML encoded HTTP responses.
 */
class PostController @Inject()(action: PostAction,
                               handler: PostResourceHandler)
                              (implicit ec: ExecutionContext)
  extends Controller {

  private val form: Form[PostFormInput] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "title" -> nonEmptyText,
        "body" -> text
      )(PostFormInput.apply)(PostFormInput.unapply)
    )
  }

  def index: Action[AnyContent] = {
    action.async { implicit request =>
      handler.find.map { posts =>
        render {
          case Accepts.Json() =>
            Ok(Json.toJson(posts))

          case Accepts.Html() =>
            Ok(views.html.posts.index(posts, form))
        }
      }
    }
  }

  def process: Action[AnyContent] = {
    action.async { implicit request =>
      request.contentType match {
        case Some(JSON) =>
          processJsonPost()

        case Some(FORM) =>
          processHtmlPost()

        case other =>
          Future.successful(UnsupportedMediaType)
      }
    }
  }

  def show(id: String): Action[AnyContent] = {
    action.async { implicit request =>
      handler.lookup(id).map {
        case Some(post) =>
          render {
            case Accepts.Json() =>
              Ok(Json.toJson(post))
            case Accepts.Html() =>
              Ok(views.html.posts.show(post))
          }

        case None =>
          NotFound
      }
    }
  }

  private def processJsonPost[A]()(implicit request: PostRequest[A]): Future[Result] = {
    def failure(badForm: Form[PostFormInput]) = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: PostFormInput) = {
      handler.create(input).map { post =>
        Created(Json.toJson(post))
          .withHeaders("Location" -> post.link)
      }
    }

    form.bindFromRequest().fold(failure, success)
  }

  private def processHtmlPost[A]()(implicit request: PostRequest[A]): Future[Result] = {
    def failure(badForm: Form[PostFormInput]) = {
      Future.successful {
        BadRequest(views.html.posts.create(badForm))
          .withFlashError("Could not create post!")
      }
    }

    def success(input: PostFormInput) = {
      handler.create(input).map { post =>
        Redirect(request.uri)
          .withFlashSuccess(s"Created post $post")
      }
    }

    form.bindFromRequest().fold(failure, success)
  }

}


