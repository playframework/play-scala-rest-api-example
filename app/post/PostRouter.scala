package post

import javax.inject.Inject

import com.lightbend.blog.post.PostId
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
 * Routes and URLs to the PostResource controller.
 */
class PostRouter @Inject()(controller: PostController)
  extends SimpleRouter {

  val prefix = "/posts"

  def link(id: PostId): String = {
    import com.netaporter.uri.dsl._
    val url = prefix / id.toString
    url.toString()
  }

  // Here, we want to route a function to a controller method,
  // without calling the method directly.  Methods in Scala are
  // not values, but functions are, so we use eta expansion (the
  // trailing underscore) to do the conversion.

  override def routes: Routes = {
    case GET(p"/") =>
      val f = controller.index _
      f()

    case POST(p"/") =>
      val f = controller.process _
      f()

    case GET(p"/$id") =>
      val f = controller.show _
      f(id)
  }

}
