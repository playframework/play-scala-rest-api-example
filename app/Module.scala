import javax.inject._

import akka.actor.ActorSystem
import com.codahale.metrics.MetricRegistry
import com.google.inject.AbstractModule
import com.lightbend.blog.comment._
import com.lightbend.blog.post._
import filters._
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment}
import post.PostActionConfig

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

    // Hook in the coda hale metrics classes
    bind(classOf[MetricRegistry]).toProvider(classOf[MetricRegistryProvider])
    bind(classOf[MetricReporter]).asEagerSingleton()
  }
}

@Singleton
class StrictTransportSecurityConfigProvider @Inject()(configuration: Configuration)
  extends Provider[StrictTransportSecurityConfig] {

  lazy val get: StrictTransportSecurityConfig = {
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
  lazy val get: PostExecutionContext = {
    val ec = actorSystem.dispatchers.lookup("restapi.postRepository.dispatcher")
    new PostExecutionContext(ec)
  }
}

@Singleton
class PostRepositoryProvider @Inject()(applicationLifecycle: ApplicationLifecycle,
                                       executionContext: PostExecutionContext)
  extends Provider[PostRepository] {
  lazy val get: PostRepository = {
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
  lazy val get: CommentExecutionContext = {
    val ec = actorSystem.dispatchers.lookup("restapi.commentRepository.dispatcher")
    new CommentExecutionContext(ec)
  }
}

@Singleton
class CommentRepositoryProvider @Inject()(applicationLifecycle: ApplicationLifecycle,
                                          executionContext: CommentExecutionContext)
  extends Provider[CommentRepository] {
  lazy val get: CommentRepository = {
    val repo = new CommentRepositoryImpl(executionContext)
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

  lazy val get: PostActionConfig = {
    PostActionConfig.fromConfiguration(configuration.underlying)
  }
}

@Singleton
class MetricRegistryProvider extends Provider[MetricRegistry] {
  lazy val get = new MetricRegistry()
}

