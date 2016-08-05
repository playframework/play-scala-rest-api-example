package circuitbreaker

import net.jodah.failsafe.{ExecutionContext => FailsafeInternalExecutionContext, _}

import scala.concurrent.Future
import scala.language.implicitConversions

/**
 * Provides a circuit breaker wrapper through Failsafe.
 */
trait CircuitBreaker extends LowPriorityFailsafeImplicits {

  import scala.compat.java8.FutureConverters._

  def failsafeBuilder: FailsafeBuilder

  object breaker {

    /**
     * Provides a synchronous interface for circuit breaker.
     *
     * {{{
     *   val failsafeBuilder = ...
     *
     *   val result = breaker { ctx =>
     *      ...
     *   }
     * }}}
     */
    def apply[A](runnable: FailsafeExecutionContext => A): A = {
      failsafeBuilder.sync.get { ctx: FailsafeInternalExecutionContext =>
        runnable(FailsafeExecutionContext(ctx))
      }
    }

    /**
     * Provides a Future based interface for circyit breaker.
     *
     * {{{
     *   val failsafeBuilder = ...
     *
     *   val future: Future[_] = breaker.async { ctx =>
     *      ...
     *   }
     * }}}
     */
    object async {
      def apply[A](runnable: FailsafeExecutionContext => Future[A]): Future[A] = {
        try {
          failsafeBuilder.async.future { ctx: FailsafeInternalExecutionContext =>
            runnable(FailsafeExecutionContext(ctx)).toJava.toCompletableFuture
          }.toScala
        } catch {
          case e: CircuitBreakerOpenException =>
            Future.failed(e)
        }
      }
    }
  }
}
