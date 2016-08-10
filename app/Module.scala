import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import javax.inject.{Inject, _}

import akka.actor.ActorSystem
import circuitbreaker.FailsafeBuilder
import com.codahale.metrics.{ConsoleReporter, MetricRegistry, Slf4jReporter}
import com.google.inject.AbstractModule
import com.lightbend.blog.comment._
import com.lightbend.blog.post._
import filters._
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
             configuration: Configuration) extends AbstractModule {
  override def configure() = {
    // Set up the HSTS filter configuration
    bind(classOf[StrictTransportSecurityConfig]).toProvider(classOf[StrictTransportSecurityConfigProvider])

    // Set up the two external datastores and their execution contexts
    bind(classOf[CommentExecutionContext]).toProvider(classOf[CommentExecutionContextProvider])
    bind(classOf[CommentRepository]).toProvider(classOf[CommentRepositoryProvider])
    bind(classOf[PostExecutionContext]).toProvider(classOf[PostExecutionContextProvider])
    bind(classOf[PostRepository]).toProvider(classOf[PostRepositoryProvider])

    // Set up the configuration for the PostAction
    bind(classOf[PostActionConfig]).toProvider(classOf[PostActionConfigProvider])
    bind(classOf[FailsafeBuilder]).toProvider(classOf[FailsafeBuilderProvider])

    // Hook in coda hale metrics
    bind(classOf[MetricRegistry]).toProvider(classOf[MetricRegistryProvider]).asEagerSingleton()
  }
}

@Singleton
class StrictTransportSecurityConfigProvider @Inject()(configuration: Configuration)
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
@Singleton
class PostExecutionContextProvider @Inject()(actorSystem: ActorSystem)
  extends Provider[PostExecutionContext] {
  override def get: PostExecutionContext = {
    val ec = actorSystem.dispatchers.lookup("restapi.postRepository.dispatcher")
    new PostExecutionContext(ec)
  }
}

@Singleton
class PostRepositoryProvider @Inject()(applicationLifecycle: ApplicationLifecycle,
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
@Singleton
class CommentExecutionContextProvider @Inject()(actorSystem: ActorSystem)
  extends Provider[CommentExecutionContext] {
  override def get: CommentExecutionContext = {
    val ec = actorSystem.dispatchers.lookup("restapi.commentRepository.dispatcher")
    new CommentExecutionContext(ec)
  }
}

@Singleton
class CommentRepositoryProvider @Inject()(applicationLifecycle: ApplicationLifecycle,
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

@Singleton
class PostActionConfigProvider @Inject()(configuration: Configuration)
  extends Provider[PostActionConfig] {

  override def get: PostActionConfig = {
    PostActionConfig.fromConfiguration(configuration.underlying)
  }
}

@Singleton
class MetricRegistryProvider @Inject()(lifecycle: ApplicationLifecycle)
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

@Singleton
class FailsafeBuilderProvider @Inject()(config: PostActionConfig,
                                        lifecycle: ApplicationLifecycle)
  extends Provider[FailsafeBuilder] {

  override def get = new FailsafeBuilder {

    import net.jodah.failsafe._

    val scheduler: ScheduledExecutorService = {
      val s = Executors.newScheduledThreadPool(2)
      lifecycle.addStopHook(() => Future.successful(s.shutdown()))
      s
    }

    val circuitBreaker = {
      val breaker = new CircuitBreaker()
        .withFailureThreshold(3)
        .withSuccessThreshold(1)
        .withDelay(5, TimeUnit.SECONDS)
        .withTimeout(config.timeout.toMillis, TimeUnit.MILLISECONDS)
      breaker
    }

    def sync[R]: SyncFailsafe[R] = {
      Failsafe.`with`(circuitBreaker)
    }

    def async[R]: AsyncFailsafe[R] = {
      sync.`with`(scheduler)
    }
  }
}
