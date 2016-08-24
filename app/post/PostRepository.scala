package post

import scala.concurrent.Future

final case class PostData(id: PostId, title: String, body: String)

class PostId private(val underlying: Int) extends AnyVal {
  override def toString: String = underlying.toString
}

object PostId {
  def apply(raw: String): PostId = {
    require(raw != null)
    new PostId(Integer.parseInt(raw))
  }
}

/**
 * A pure non-blocking interface for the PostRepository.
 */
trait PostRepository {
  def create(data: PostData): Future[PostId]

  def list(): Future[Iterable[PostData]]

  def get(id: PostId): Future[Option[PostData]]
}
