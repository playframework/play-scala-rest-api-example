package post

import javax.inject.Inject

import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
 * Routes to the post controller.
 */
class PostRouter @Inject()(controller: PostController)
  extends SimpleRouter {

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
