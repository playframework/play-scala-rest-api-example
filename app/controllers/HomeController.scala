package controllers

import javax.inject.Inject

import play.api.mvc.{Action, Controller}
import post.PostRouter

class HomeController @Inject()(postRouter: PostRouter) extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.index(postRouter))
  }

}
