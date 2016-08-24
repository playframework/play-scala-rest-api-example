import javax.inject._

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import play.api.{Configuration, Environment}
import post._

import scala.concurrent.Future

/**
 * Sets up custom components for Play.
 *
 * https://www.playframework.com/documentation/2.5.x/ScalaDependencyInjection
 */
class Module(environment: Environment,
             configuration: Configuration) extends AbstractModule with ScalaModule {

  override def configure() = {
    bind[PostRepository].to[PostRepositoryImpl].in[Singleton]
  }
}
