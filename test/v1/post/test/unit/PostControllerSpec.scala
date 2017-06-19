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
import play.api.libs.json.Json


class PostControllerSpec extends PlaySpec with Results with MockitoSugar {

  // mock data
  val mockList = List(
    PostResource("1", "post/1", "title 1", "blog post 1"),
    PostResource("2", "post/2", "title 2", "blog post 2"),
    PostResource("3", "post/3", "title 3", "blog post 3"),
    PostResource("4", "post/4", "title 4", "blog post 4"),
    PostResource("5", "post/5", "title 5", "blog post 5")
  )

  // Fixtures
  val messagesApi = mock[MessagesApi]
  val action = new PostAction(messagesApi)
  val handler = mock[PostResourceHandler]
  
  "index" should {
    "be valid" in {
      when(handler.find) thenReturn Future.successful(mockList.toIterable)     
      val controller = new PostController(action, handler)
      val result: Future[Result] = controller.index().apply(FakeRequest())            
      val json = contentAsJson(result)      
      val elem0 = (json)(0)
      
      status(result) must be(OK)
      (elem0 \ "id").as[String] mustBe "1"
      (elem0 \ "title").as[String] mustBe "title 1"
      (elem0 \ "link").as[String] mustBe "post/1"
      (elem0 \ "body").as[String] mustBe "blog post 1"
    }
  }

  "show" should {
    "be valid" in {
      when(handler.lookup("1")) thenReturn Future.successful(Some(mockList(0)))     
      val controller = new PostController(action, handler)
      val result: Future[Result] = controller.show("1").apply(FakeRequest())
      val json = contentAsJson(result)      
      
      status(result) must be(OK)
      (json \ "id").as[String] mustBe "1"
      (json \ "title").as[String] mustBe "title 1"
      (json \ "link").as[String] mustBe "post/1"
      (json \ "body").as[String] mustBe "blog post 1"
    }
  }
    
  "process" should {
    "should be valid" in {      
      val post = PostFormInput("title 1", "blog post 1")
      when(handler.create(post)) thenReturn Future.successful(mockList(0))
      val controller = new PostController(action, handler)
      val request = FakeRequest(POST, "/").withJsonBody(Json.parse("""{"title":"title 1","body":"blog post 1"}"""))      
      val result: Future[Result] = controller.process.apply(request)
      val json = contentAsJson(result)            
      
      status(result) must be(CREATED)
      (json \ "id").as[String] mustBe "1"
      (json \ "title").as[String] mustBe "title 1"
      (json \ "link").as[String] mustBe "post/1"
      (json \ "body").as[String] mustBe "blog post 1"
   }
  }

}