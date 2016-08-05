package post

import javax.inject.Inject
import java.util.concurrent._

import com.lightbend.blog.comment._
import com.lightbend.blog.post._
import circuitbreaker._

import play.api.data.Form
import play.api.data.Forms._
import play.api.http.MimeTypes
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.Json
import play.api.mvc.Results._
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
                           commentRepository: CommentRepository,
                           lifecycle: ApplicationLifecycle)(implicit ec: ExecutionContext)
  extends SimpleRouter with RequestExtractors with Rendering with CircuitBreaker {

  val failsafeBuilder = new FailsafeBuilder {
    import net.jodah.failsafe._

    val scheduler: ScheduledExecutorService = {
      val s = Executors.newScheduledThreadPool(2)
      lifecycle.addStopHook(() => Future.successful(s.shutdown()))
      s
    }

    val circuitBreaker = {
      val breaker = new CircuitBreaker()
        .withFailureThreshold(3)
        .withSuccessThreshold(1)
        .withDelay(5, TimeUnit.SECONDS)
      breaker
    }

    def sync[R]: SyncFailsafe[R] = {
      Failsafe.`with`(circuitBreaker)
    }

    def async[R]: AsyncFailsafe[R] = {
      sync.`with`(scheduler)
    }
  }

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
    request.contentType match {
      case Some(MimeTypes.JSON) =>
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

      case Some(MimeTypes.FORM) =>
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
        Future.successful(UnsupportedMediaType)
    }
  }

  private def renderPost[A](id: PostId)(implicit request: PostRequest[A]): Future[Result] = {
    logger.trace("renderPost: ")
    // Find a single item from the repository
    render.async {

      case Accepts.Json() & Accepts.Html() if request.method == "HEAD" =>
        // Do not render a body on HEAD
        findPost(id).flatMap {
          case Some(p) =>
            Future.successful(Results.Ok)
          case None =>
            Future.successful(Results.NotFound)
        }

      case Accepts.Json() =>
        findPost(id).flatMap {
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
        findPost(id).flatMap {
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
        findPosts().map { posts =>
          val json = Json.toJson(posts)
          Results.Ok(json)
        }

      case Accepts.Html() =>
        // Query the repository for available posts
        findPosts().map { posts =>
          Results.Ok(views.html.posts.index(posts, form))
        }
    }
  }

  private def createPost[A](postInput: PostFormInput): Future[Post] = {
    breaker.async { _ =>
      val data = PostData(PostId("999"), postInput.title, postInput.body)
      // we don't actually create the post, so return what we have
      postRepository.create(data).map { id =>
        Post(data, Seq.empty)
      }
    }
  }

  private def findPost[A](id: PostId): Future[Option[PostData]] = {
    breaker.async { _ =>
      postRepository.get(id)
    }
  }

  private def findPosts(): Future[Iterable[Post]] = {
    breaker.async { _ =>
      postRepository.list().flatMap { postDataList =>
        // Get an Iterable[Future[Post]] containing comments
        val listOfFutures = postDataList.map { p =>
          findComments(p.id).map { comments =>
            Post(p, comments)
          }
        }

        // Flip it into a single Future[Iterable[Post]]
        Future.sequence(listOfFutures)
      }
    }
  }

  private def findComments(postId: PostId): Future[Seq[Comment]] = {
    breaker.async { _ =>
      // Find all the comments for this post
      commentRepository.findByPost(postId.toString).map { comments =>
        comments.map(c => Comment(c.body)).toSeq
      }
    }
  }

}
