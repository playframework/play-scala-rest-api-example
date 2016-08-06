package post

import java.util.concurrent._
import javax.inject.Inject

import akka.actor.ActorSystem
import com.codahale.metrics.MetricRegistry
import net.jodah.failsafe.CircuitBreakerOpenException
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

  override def invokeBlock[A](request: Request[A], block: PostRequestBlock[A]): Future[Result] = {
    if (logger.isTraceEnabled) {
      logger.trace(s"invokeBlock: request = $request")
    }

    // Count the number of requests
    requestsMeter.mark()

    // Fail the backend futures if they take more than the timeout
    withTimeout {
      // Measures the wall clock time to execute the action
      wallClockTimer.timeFuture {
        val messages = messagesApi.preferred(request)
        val postRequest = new PostRequest(request, messages)
        block(postRequest).map { result =>
          postRequest.method match {
            case "GET" | "HEAD" =>
              result.withHeaders(("Cache-Control", s"max-age: $maxAge"))
            case other =>
              result
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
 * A wrapped request for post resources.
 *
 * This is commonly used to hold request-specific information like
 * security credentials, and useful shortcut methods.
 */
class PostRequest[A](request: Request[A], val messages: Messages)
  extends WrappedRequest(request) {
  def flashSuccess: Option[String] = request.flash.get("success")

  def flashError: Option[String] = request.flash.get("error")
}
