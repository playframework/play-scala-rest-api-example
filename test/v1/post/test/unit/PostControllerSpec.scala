package v1.post.test.unit

import scala.concurrent.Future

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._

import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import v1.post.PostAction
import v1.post.PostController
import v1.post.PostResourceHandler
import v1.post.PostResourceHandler
import v1.post.PostId
import v1.post.PostData
import org.mockito.Mockito._
import v1.post.PostResource
import v1.post.PostFormInput

class PostControllerSpec extends PlaySpec with Results with MockitoSugar {
  
  private val mockList = List(
    PostResource("1", "post/1", "title 1", "blog post 1"),
    PostResource("2", "post/2", "title 2", "blog post 2"),
    PostResource("3", "post/3", "title 3", "blog post 3"),
    PostResource("4", "post/4", "title 4", "blog post 4"),
    PostResource("5", "post/5", "title 5", "blog post 5")
  )

  "index" should {
    "be valid" in {
      val messagesApi = mock[MessagesApi]
      val action = new PostAction(messagesApi)
      val handler = mock[PostResourceHandler]
      when(handler.find) thenReturn Future.successful(mockList.toIterable)     
      val controller = new PostController(action, handler)
      val result: Future[Result] = controller.index().apply(FakeRequest())      
      status(result) must be (OK)
      val text = contentAsString(result)
      text != null mustBe true
      text.startsWith("[{\"id\":\"1\",\"link\":\"post/1\",\"title\":\"title 1\",\"body\"") mustBe true
    }
  }

  "show" should {
    "be valid" in {
      val messagesApi = mock[MessagesApi]
      val action = new PostAction(messagesApi)
      val handler = mock[PostResourceHandler]
      when(handler.lookup("1")) thenReturn Future.successful(Some(mockList(0)))     
      val controller = new PostController(action, handler)
      val result: Future[Result] = controller.show("1").apply(FakeRequest())      
      status(result) must be (OK)
      val text = contentAsString(result)
      text != null mustBe true
      text.startsWith("{\"id\":\"1\",\"link\":\"post/1\",\"title\":\"title 1\",\"body\"") mustBe true
    }
  }

}