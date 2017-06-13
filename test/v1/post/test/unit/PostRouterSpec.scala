package v1.post.test.unit

import org.scalatest.FlatSpec
import org.scalatest.Matchers

import org.scalatest._
import org.scalatestplus.play._

import org.scalatest.mockito.MockitoSugar 
import v1.post.PostController
import v1.post.PostId
import play.api.routing.sird._
import play.mvc.Action
import play.api.mvc.AnyContent
import v1.post.PostRouter

class PostRouterSpec extends PlaySpec with MockitoSugar {
  
  "PostRouter#link" should {
    "return link" in {
      val mockCtrl = mock[PostController]      
      val obj = new PostRouter(mockCtrl)

      val v = obj.link(PostId("1"))
      v mustEqual "/v1/posts/1"
    }
  }
  
  "PostRouter#routes" should {
    "return routes" in {
      
      val mockCtrl = mock[PostController]
      val mockAction = mock[Action[AnyContent]]

      val obj = new PostRouter(mockCtrl)
      val routes = obj.routes      
      
      (routes != null) mustBe true
    }
  }
  
}