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

  override def routes: Routes = {
    case GET(p"/") =>
      controller.index

    case POST(p"/") =>
      controller.process

    case GET(p"/$id") =>
      controller.show(id)
  }

}
