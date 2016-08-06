package post

import javax.inject.Inject

import play.api.data.Form
import play.api.data.Forms._
import play.api.http.MimeTypes
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

import scala.concurrent.{ExecutionContext, Future}

case class PostFormInput(title: String, body: String)

/**
 * Takes HTTP requests and produces JSON or HTML responses.
 */
class PostRouter @Inject()(action: PostAction,
                           postHandler: PostHandler)
                          (implicit ec: ExecutionContext)
  extends SimpleRouter with AcceptExtractors with Rendering {

  private val form = Form(
    mapping(
      "title" -> nonEmptyText,
      "body" -> text
    )(PostFormInput.apply)(PostFormInput.unapply)
  )

  override def routes: Routes = {
    case GET(p"/") =>
      action.async { implicit request =>
        renderPosts()
      }

    case POST(p"/") =>
      action.async { implicit request =>
        processPost()
      }

    case GET(p"/$id") =>
      action.async { implicit request =>
        renderPost(id)
      }

  }

  private def renderPosts[A]()(implicit request: PostRequest[A]): Future[Result] = {
    render.async {

      case Accepts.Json() =>
        postHandler.find.map { posts =>
          Results.Ok(Json.toJson(posts))
        }

      case Accepts.Html() =>
        postHandler.find.map { posts =>
          Results.Ok(views.html.posts.index(posts, form))
        }
    }
  }

  private def processPost[A]()(implicit request: PostRequest[A]): Future[Result] = {
    request.contentType match {
      case Some(MimeTypes.JSON) =>
        processJsonPost()

      case Some(MimeTypes.FORM) =>
        processHtmlPost()

      case other =>
        Future.successful(UnsupportedMediaType)
    }
  }

  private def processJsonPost[A]()(implicit request: PostRequest[A]): Future[Result] = {
    def failure(badForm: Form[PostFormInput]) = {
      Future.successful(Results.BadRequest(badForm.errorsAsJson))
    }

    def success(input: PostFormInput) = {
      postHandler.create(input).map { post =>
        Results.Created(Json.toJson(post))
          .withHeaders("Location" -> post.link)
      }
    }

    form.bindFromRequest().fold(failure, success)
  }

  private def processHtmlPost[A]()(implicit request: PostRequest[A]): Future[Result] = {
    def failure(badForm: Form[PostFormInput]) = {
      Future.successful {
        Results.BadRequest(views.html.posts.create(badForm))
          .withFlashError("Could not create post!")
      }
    }

    def success(input: PostFormInput) = {
      postHandler.create(input).map { post =>
        Results.Redirect(request.uri)
          .withFlashSuccess(s"Created post $post")
      }
    }

    form.bindFromRequest().fold(failure, success)
  }

  private def renderPost[A](id: String)(implicit request: PostRequest[A]): Future[Result] = {
    render.async {

      case Accepts.Json() =>
        postHandler.lookup(id).map {
          case Some(post) =>
            Results.Ok(Json.toJson(post))
          case None =>
            Results.NotFound
        }

      case Accepts.Html() =>
        postHandler.lookup(id).map {
          case Some(post) =>
            Results.Ok(views.html.posts.show(post))
          case None =>
            Results.NotFound
        }
    }
  }

}


