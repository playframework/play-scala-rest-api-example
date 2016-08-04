package post

import java.util.concurrent._
import javax.inject.Inject

import akka.actor.ActorSystem
import com.codahale.metrics.MetricRegistry
import net.jodah.failsafe._
import nl.grons.metrics.scala.InstrumentedBuilder
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, TimeoutException}

/**
 * The default action for the Post resource.
 *
 * This is the place to put logging, metrics, to augment
 * the request with contextual data, and manipulate the
 * result.
 */
class PostAction @Inject()(config: PostActionConfig,
                           messagesApi: MessagesApi,
                           actorSystem: ActorSystem,
                           val metricRegistry: MetricRegistry)(implicit ec: ExecutionContext)
  extends ActionBuilder[PostRequest] with InstrumentedBuilder {

  type PostRequestBlock[A] = (PostRequest[A]) => Future[Result]

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  private val requestsMeter = metrics.meter("requests-meter", "requests")

  private val wallClockTimer = metrics.timer("wall-clock")

  private val maxAge = config.maxAge.getSeconds

  private val timeoutDuration = FiniteDuration(config.timeout.toMillis, TimeUnit.MILLISECONDS)

  val scheduler: ScheduledExecutorService = {
    Executors.newScheduledThreadPool(Runtime.getRuntime.availableProcessors)
  }
  
  private val breaker = new CircuitBreaker()
    .withFailureThreshold(3, 10)
    .withSuccessThreshold(5)
    .withDelay(1, TimeUnit.MINUTES)

  def withCircuitBreaker[A]: Retry[A] = {
    val policy = new RetryPolicy().withMaxRetries(0)
    new Retry(Failsafe.`with`(policy).`with`(scheduler).`with`(breaker))
  }

  override def invokeBlock[A](request: Request[A], block: PostRequestBlock[A]): Future[Result] = {
    if (logger.isTraceEnabled) {
      logger.trace(s"invokeBlock: request = $request")
    }

    // Count the number of requests
    requestsMeter.mark()

    // Fail the backend futures if they take more than the timeout
    withTimeout {
      // Wraps request in circuit breaker block (note: you may not want this for POST)
      withCircuitBreaker {
        // Measures the wall clock time to execute the action
        wallClockTimer.timeFuture {
          val messages = messagesApi.preferred(request)
          val postRequest = new PostRequest(request, messages)
          block(postRequest).map { result =>
            if (postRequest.method == "GET") {
              result.withHeaders(("Cache-Control", s"max-age: $maxAge"))
            } else {
              result
            }
          }
        }
      }
    }.recoverWith {
      case e: CircuitBreakerOpenException =>
        logger.warn(s"Circuit breaker open on request $request")
        Future.successful(Results.ServiceUnavailable)

      case e: TimeoutException =>
        logger.error(s"Future timeout on request $request", e)
        Future.successful(Results.ServiceUnavailable)
    }
  }

  private def withTimeout[A](result: => Future[Result]): Future[Result] = {
    val timeoutResult = akka.pattern.after(timeoutDuration, actorSystem.scheduler) {
      val msg = s"Timeout after $timeoutDuration"
      Future.failed(new TimeoutException(msg))
    }(actorSystem.dispatchers.defaultGlobalDispatcher)
    Future.firstCompletedOf(Seq(result, timeoutResult))
  }
}

/**
 * A wrapped request that can contain Post information.
 *
 * This is commonly used to hold request-specific information like
 * security credentials, and useful shortcut methods.
 */
class PostRequest[A](request: Request[A], val messages: Messages)
  extends WrappedRequest(request) {
  def flashSuccess: Option[String] = request.flash.get("success")

  def flashError: Option[String] = request.flash.get("error")
}

/**
 * A helper class to turn a Failsafe call into a Scala future.
 */
class Retry[A](failsafe: => AsyncFailsafe[A]) {
  def apply(runnable: => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    import scala.compat.java8.FutureConverters._

    try {
      val callable = new Callable[CompletableFuture[A]]() {
        def call(): CompletableFuture[A] = runnable.toJava.toCompletableFuture
      }
      failsafe.future(callable).toScala
    } catch {
      case e: CircuitBreakerOpenException =>
        Future.failed(e)
    }
  }
}
