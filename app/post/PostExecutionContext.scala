package post

import scala.concurrent.ExecutionContext

/**
 * A typed execution context for the PostRepository.
 *
 * An execution context provides access to an Executor, but it's important
 * that the thread pool is sized appropriately to the underlying implementation.
 * For example, if you are using JDBC or a similar blocking model, then you will
 * need a ThreadPoolExecutor with a fixed size equal to the maximum number of JDBC
 * connections in the JDBC connection pool (i.e. HikariCP).
 *
 * Because ExecutionContext is often passed round implicitly and it's not widely
 * known, it's much better to ensure that anything Repository based has a custom
 * strongly typed execution context so that an inappropriate ExecutionContext can't
 * be used by accident.
 */
class PostExecutionContext(val underlying: ExecutionContext)

