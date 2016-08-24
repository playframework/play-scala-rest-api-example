package post

import scala.concurrent.Future

/**
 * Controls any resources owned by the post repository.
 */
trait PostRepositoryLifecycle {

  def stop(): Future[Unit]

}
