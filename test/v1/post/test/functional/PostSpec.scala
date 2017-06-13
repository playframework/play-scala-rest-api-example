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

class PostSpec extends PlaySpec with GuiceOneAppPerSuite {

  // Override fakeApplication if you need a Application with other than
  // default parameters.
  override def fakeApplication() = new GuiceApplicationBuilder().build()

  "The GuiceOneAppPerSuite trait" must {
    "start the Application" in {
      app.configuration != null mustBe true
      app.configuration.getConfig("play") != null mustBe true
    }
  }
}
