package post

import javax.inject.Inject

import com.lightbend.blog.comment._
import com.lightbend.blog.post._
import play.api.data.Form
import play.api.data.Forms._
import play.api.http.MimeTypes
import play.api.libs.json.Json
import play.api.mvc._
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

import scala.concurrent.{ExecutionContext, Future}

case class PostFormInput(title: String, body: String)

/**
 * Takes HTTP requests and produces JSON or HTML responses
 * from a repository providing data.
 */
class PostRouter @Inject()(action: PostAction,
                           postRepository: PostRepository,
                           commentRepository: CommentRepository)(implicit ec: ExecutionContext)
  extends SimpleRouter with RequestExtractors with Rendering {

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

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

    case HEAD(p"/") =>
      action.async { implicit request =>
        renderPosts()
      }

    case POST(p"/") =>
      action.async { implicit request =>
        processPost()
      }

    case GET(p"/$id") =>
      action.async { implicit request =>
        renderPost(PostId(id))
      }

    case HEAD(p"/$id") =>
      action.async { implicit request =>
        renderPost(PostId(id))
      }
  }

  private def processPost[A]()(implicit request: PostRequest[A]): Future[Result] = {
    // Match against the content type so we can behave like render.async
    private object process {
      case class Processing(mimeType: String) {
        def unapply(request: RequestHeader): Boolean = {
          request.contentType.contains(mimeType)
        }
      }

      val Json = Processing(MimeTypes.JSON)
      val Form = Processing(MimeTypes.FORM)
    }

    request.contentType match {
      case process.Json() =>
        form.bindFromRequest().fold(hasErrors = { badForm =>
          Future.successful {
            Results.BadRequest(badForm.errorsAsJson)
          }
        }, success = { postInput =>
          createPost(postInput).map { post =>
            Results.Created(Json.toJson(post))
              .withHeaders("Location" -> post.link)
          }
        })

      case process.Form() =>
        form.bindFromRequest().fold(hasErrors = { badForm =>
          Future.successful {
            Results.BadRequest(views.html.posts.create(badForm))
              .withFlashError("Could not create post!")
          }
        }, success = { postInput =>
          createPost(postInput).map { post =>
            Results.Redirect(request.uri)
              .withFlashSuccess(s"Created post $post")
          }
        })

      case other =>
        // For input content, we want to send 415, when they send an unacceptable content type.
        Future.successful {
          Results.UnsupportedMediaType
        }
    }
  }

  private def createPost[A](postInput: PostFormInput)(implicit request: PostRequest[A]): Future[Post] = {
    logger.trace(s"createPost: postInput = $postInput")
    Future.successful {
      val id = PostId("999")
      Post(id.toString(), Post.link(id), "fake title", "fake body", Seq.empty)
    }
  }

  private def renderPost[A](id: PostId)(implicit request: PostRequest[A]): Future[Result] = {
    logger.trace("renderPost: ")
    // Find a single item from the repository
    render.async {

      case Accepts.Json() & Accepts.Html() if request.method == "HEAD" =>
        // Do not render a body on HEAD
        postRepository.get(id).flatMap {
          case Some(p) =>
            Future.successful(Results.Ok)
          case None =>
            Future.successful(Results.NotFound)
        }

      case Accepts.Json() =>
        postRepository.get(id).flatMap {
          case Some(p) =>
            findComments(p.id).map { comments =>
              val post = Post(p, comments)
              val json = Json.toJson(post)
              Results.Ok(json)
            }
          case None =>
            Future.successful(Results.NotFound)
        }

      case Accepts.Html() =>
        postRepository.get(id).flatMap {
          case Some(p) =>
            findComments(p.id).map { comments =>
              val post = Post(p, comments)
              Results.Ok(views.html.posts.show(post))
            }
          case None =>
            Future.successful(Results.NotFound)
        }

    }
  }

  private def renderPosts[A]()(implicit request: PostRequest[A]): Future[Result] = {
    render.async {

      case Accepts.Json() & Accepts.Html() if request.method == "HEAD" =>
        // HEAD has no body, so just say hi
        Future.successful(Results.Ok)

      case Accepts.Json() =>
        // Query the repository for available posts
        postRepository.list().flatMap { postDataList =>
          findPosts(postDataList).map { posts =>
            val json = Json.toJson(posts)
            Results.Ok(json)
          }
        }

      case Accepts.Html() =>
        // Query the repository for available posts
        postRepository.list().flatMap { postDataList =>
          findPosts(postDataList).map { posts =>
            Results.Ok(views.html.posts.index(posts, form))
          }
        }

    }
  }

  private def findPosts(postDataList: Iterable[PostData]): Future[Iterable[Post]] = {
    // Get an Iterable[Future[Post]] containing comments
    val listOfFutures = postDataList.map { p =>
      findComments(p.id).map { comments =>
        Post(p, comments)
      }
    }

    // Flip it into a single Future[Iterable[Post]]
    Future.sequence(listOfFutures)
  }

  private def findComments(postId: PostId): Future[Seq[Comment]] = {
    // Find all the comments for this post
    commentRepository.findByPost(postId.toString).map { comments =>
      comments.map(c => Comment(c.body)).toSeq
    }
  }

}
