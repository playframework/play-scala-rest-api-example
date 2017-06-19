package v1.post.test.functional

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{GET => GET_REQUEST, _}
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.routing.sird._
import play.api.routing._
import play.api.mvc._
import play.api.mvc.Results._

class PostServerSpec extends PlaySpec with GuiceOneServerPerSuite {
  
  "test server logic" in {
    val wsClient = app.injector.instanceOf[WSClient]
    val server =  s"localhost:$port"
    val testURL = s"http://$server/v1/posts"
    val response = await(wsClient.url(testURL).get())
    val json = response.json
    val elem0 = (json)(0)
    
    response.status mustBe OK
    response.header("Content-Type") mustBe Some("application/json")

    (elem0 \ "id").as[String] mustBe "1"
    (elem0 \ "title").as[String] mustBe "title 1"
    (elem0 \ "link").as[String] mustBe "/v1/posts/1"
    (elem0 \ "body").as[String] mustBe "blog post 1"    
  }
}