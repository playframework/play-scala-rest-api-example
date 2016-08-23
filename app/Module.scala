import java.util.concurrent._
import javax.inject._

import akka.actor.ActorSystem
import com.codahale.metrics._
import com.google.inject.AbstractModule
import com.lightbend.blog.comment._
import com.lightbend.blog.post._
import filters.StrictTransportSecurityConfig
import net.codingwell.scalaguice.ScalaModule
import org.slf4j.LoggerFactory
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment}
import post.PostActionConfig

import scala.concurrent.Future

/**
 * Sets up custom components for Play.
 *
 * https://www.playframework.com/documentation/2.5.x/ScalaDependencyInjection
 */
class Module(environment: Environment,
             configuration: Configuration) extends AbstractModule with ScalaModule {

  import Module._

  override def configure() = {
    // Set up the HSTS filter configuration
    bind[StrictTransportSecurityConfig].toProvider[StrictTransportSecurityConfigProvider].in[Singleton]

    // Set up the two external datastores and their execution contexts
    bind[CommentExecutionContext].toProvider[CommentExecutionContextProvider].in[Singleton]
    bind[CommentRepository].toProvider[CommentRepositoryProvider].in[Singleton]
    bind[PostExecutionContext].toProvider[PostExecutionContextProvider].in[Singleton]
    bind[PostRepository].toProvider[PostRepositoryProvider].in[Singleton]

    // Set up the configuration for the PostAction
    bind[PostActionConfig].toProvider[PostActionConfigProvider].in[Singleton]

    // Hook in coda hale metrics
    bind[MetricRegistry].toProvider[MetricRegistryProvider].asEagerSingleton()
  }
}

object Module {

  private class StrictTransportSecurityConfigProvider @Inject()(configuration: Configuration)
    extends Provider[StrictTransportSecurityConfig] {
    override def get: StrictTransportSecurityConfig = {
      StrictTransportSecurityConfig.fromConfiguration(configuration.underlying)
    }
  }

  /**
   * Pulls a custom thread pool for the PostRepository.
   *
   * https://www.playframework.com/documentation/2.5.x/ThreadPools#Using-other-thread-pools
   */
  private class PostExecutionContextProvider @Inject()(actorSystem: ActorSystem)
    extends Provider[PostExecutionContext] {
    override def get: PostExecutionContext = {
      val ec = actorSystem.dispatchers.lookup("restapi.postRepository.dispatcher")
      new PostExecutionContext(ec)
    }
  }

  private class PostRepositoryProvider @Inject()(applicationLifecycle: ApplicationLifecycle,
                                                 executionContext: PostExecutionContext)
    extends Provider[PostRepository] {
    override def get: PostRepository = {
      val repo = new PostRepositoryImpl(executionContext)
      // Hooks the repository lifecycle to Play's lifecycle, so any resources are shutdown
      applicationLifecycle.addStopHook { () =>
        repo.stop()
      }
      repo
    }
  }

  /**
   * Pulls a custom thread pool for the CommentRepository.
   *
   * https://www.playframework.com/documentation/2.5.x/ThreadPools#Using-other-thread-pools
   */
  private class CommentExecutionContextProvider @Inject()(actorSystem: ActorSystem)
    extends Provider[CommentExecutionContext] {
    override def get: CommentExecutionContext = {
      val ec = actorSystem.dispatchers.lookup("restapi.commentRepository.dispatcher")
      new CommentExecutionContext(ec)
    }
  }

  private class CommentRepositoryProvider @Inject()(applicationLifecycle: ApplicationLifecycle,
                                                    executionContext: CommentExecutionContext)
    extends Provider[CommentRepository] {
    override def get: CommentRepository = {
      def repo = new CommentRepositoryImpl(executionContext)
      // Hooks the repository lifecycle to Play's lifecycle, so any resources are shutdown
      applicationLifecycle.addStopHook { () =>
        repo.stop()
      }
      repo
    }
  }

  private class PostActionConfigProvider @Inject()(configuration: Configuration)
    extends Provider[PostActionConfig] {

    override def get: PostActionConfig = {
      PostActionConfig.fromConfiguration(configuration.underlying)
    }
  }

  private class MetricRegistryProvider @Inject()(lifecycle: ApplicationLifecycle)
    extends Provider[MetricRegistry] {

    private val registry = new MetricRegistry()

    private lazy val consoleReporter = {
      ConsoleReporter.forRegistry(registry)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .build()
    }

    private lazy val slf4jReporter = {
      val logger = LoggerFactory.getLogger("metrics")

      Slf4jReporter.forRegistry(registry)
        .outputTo(logger)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .build()
    }

    private val reporters = Seq(slf4jReporter)

    private def start(): Unit = {
      lifecycle.addStopHook { () =>
        Future.successful(stop())
      }
      reporters.foreach(_.start(1, TimeUnit.SECONDS))
    }

    private def stop() = {
      reporters.foreach(_.stop())
    }

    override def get = {
      start()
      registry
    }
  }
}
