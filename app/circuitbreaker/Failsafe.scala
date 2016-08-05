package circuitbreaker

import net.jodah.failsafe.function._
import net.jodah.failsafe.util.Duration
import net.jodah.failsafe.{ExecutionContext => FailsafeInternalExecutionContext, _}

import scala.language.implicitConversions

/**
 * Provides a useful builder for CircuitBreaker.
 */
trait FailsafeBuilder {

  def sync[A]: SyncFailsafe[A]

  def async[A]: AsyncFailsafe[A]

}

/**
 * This trait provides implicit conversions for Failsafe function classes to
 * Scala functions.
 */
trait LowPriorityFailsafeImplicits {

  implicit def blockToAsyncCallable[R](block: AsyncExecution => R): AsyncCallable[R] = {
    new AsyncCallable[R] {
      override def call(execution: AsyncExecution): R = block(execution)
    }
  }

  implicit def blockToAsyncRunnable(block: AsyncExecution => Unit): AsyncRunnable = {
    new AsyncRunnable {
      override def run(execution: AsyncExecution): Unit = block(execution)
    }
  }

  implicit def blockToBiPredicate[T, U](block: (T, U) => Boolean): BiPredicate[T, U] = {
    new BiPredicate[T, U] {
      override def test(t: T, u: U): Boolean = block(t, u)
    }
  }

  implicit def blockToCheckedBiConsumer[T, U](block: (T, U) => Boolean): CheckedBiConsumer[T, U] = {
    new CheckedBiConsumer[T, U] {
      override def accept(t: T, u: U): Unit = block(t, u)
    }
  }

  implicit def blockToCheckedBiFunction[T, U, R](block: (T, U) => R): CheckedBiFunction[T, U, R] = {
    new CheckedBiFunction[T, U, R] {
      override def apply(t: T, u: U): R = block(t, u)
    }
  }

  implicit def blockToCheckedConsumer[T](block: T => Unit): CheckedConsumer[T] = {
    new CheckedConsumer[T] {
      def accept(t: T): Unit = block(t)
    }
  }

  implicit def blockToChainedFunction[T, R](block: T => R): CheckedFunction[T, R] = {
    new CheckedFunction[T, R] {
      override def apply(t: T): R = block(t)
    }
  }

  implicit def blockToCheckedRunnable(block: => Unit): CheckedRunnable = {
    new CheckedRunnable {
      override def run(): Unit = block
    }
  }

  implicit def blockToContextualCallable[R](block: FailsafeInternalExecutionContext => R): ContextualCallable[R] = {
    new ContextualCallable[R] {
      override def call(context: FailsafeInternalExecutionContext): R = block(context)
    }
  }

  implicit def blockToContextualRunnable(block: FailsafeInternalExecutionContext => Unit): ContextualRunnable = {
    new ContextualRunnable {
      override def run(context: FailsafeInternalExecutionContext): Unit = block(context)
    }
  }

  implicit def blockToPredicate[T](block: T => Boolean): Predicate[T] = {
    new Predicate[T] {
      override def test(t: T): Boolean = block(t)
    }
  }

}

/**
 * A failsafe execution context case class that can be used safely.
 *
 * The internal class returns an instance of AsyncExecution with internal
 * classes and no toString methods, so this is a better way to show
 * Failsafe's current state.
 */
case class FailsafeExecutionContext(executions: Int,
                                    startTime: java.time.Duration,
                                    elapsedTime: java.time.Duration) {
  override def toString: String = {
    s"FailsafeExecutionContext(executions = $executions, startTime = $startTime, elapsedTime = $elapsedTime)"
  }
}

object FailsafeExecutionContext {
  def apply(underlying: FailsafeInternalExecutionContext): FailsafeExecutionContext = {
    def elapsedTime: Duration = underlying.getElapsedTime
    def executions: Int = underlying.getExecutions
    def startTime: Duration = underlying.getStartTime
    FailsafeExecutionContext(
      executions,
      java.time.Duration.ofNanos(startTime.toNanos),
      java.time.Duration.ofNanos(elapsedTime.toNanos))
  }
}
