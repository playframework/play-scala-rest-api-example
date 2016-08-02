package post

import javax.inject.Inject

import com.codahale.metrics.MetricRegistry
import nl.grons.metrics.scala.InstrumentedBuilder
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
 * The default action for the Post resource.
 *
 * This is the place to put logging, metrics, to augment
 * the request with contextual data, and manipulate the
 * result.
 */
class PostAction @Inject()(config: PostActionConfig,
                           messagesApi: MessagesApi,
                           val metricRegistry: MetricRegistry)(implicit ec: ExecutionContext)
  extends ActionBuilder[PostRequest] with InstrumentedBuilder {

  type PostRequestBlock[A] = (PostRequest[A]) => Future[Result]

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  private val requestsMeter = metrics.meter("requests-meter", "requests")

  private val wallClockTimer = metrics.timer("wall-clock")

  private val maxAge = config.maxAge.getSeconds

  override def invokeBlock[A](request: Request[A], block: PostRequestBlock[A]): Future[Result] = {
    if (logger.isTraceEnabled) {
      logger.trace(s"invokeBlock: request = $request")
    }

    // Count the number of requests
    requestsMeter.mark()

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
