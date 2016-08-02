import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

import com.codahale.metrics.{ConsoleReporter, MetricRegistry, Slf4jReporter}
import org.slf4j.LoggerFactory
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

/**
 * Writes metrics reports into SLF4J.
 */
@Singleton
class MetricReporter @Inject()(lifecycle: ApplicationLifecycle,
                               registry: MetricRegistry) {

  private val logger = LoggerFactory.getLogger("metrics")

  val reporters = Seq(slf4jReporter)

  private def consoleReporter = {
    ConsoleReporter.forRegistry(registry)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build()
  }

  private def slf4jReporter = Slf4jReporter.forRegistry(registry)
    .outputTo(logger)
    .convertRatesTo(TimeUnit.SECONDS)
    .convertDurationsTo(TimeUnit.MILLISECONDS)
    .build()

  def start(): Unit = {
    reporters.foreach(_.start(1, TimeUnit.SECONDS))
  }

  def stop() = {
    reporters.foreach(_.stop())
  }

  lifecycle.addStopHook { () =>
    Future.successful(stop())
  }

  start()

}
