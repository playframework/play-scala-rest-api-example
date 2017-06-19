package v1.post.test.functional

import play.api.test._
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.{ Application, Play }
import play.api.inject.guice._
import play.api.routing._
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.WebDriver
import org.scalatest.Matchers
import org.scalatest.selenium.WebBrowser
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class PostBrowserSpec extends PlaySpec with GuiceOneServerPerSuite with OneBrowserPerSuite with HtmlUnitFactory {

  override def fakeApplication() = new GuiceApplicationBuilder().build()
  
  "The OneBrowserPerTest trait" must {
    "provide a web driver" in {
      go to s"http://localhost:$port/"
      pageTitle mustBe "Play REST API"
    }
  }
  
}
