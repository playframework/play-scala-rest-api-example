package post

import javax.inject.{Inject, Singleton}

import scala.concurrent._

/**
 * A trivial implementation for the Post Repository.
 */
@Singleton
class PostRepositoryImpl @Inject() extends PostRepository {

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  private val postList = List(
    PostData(PostId("1"), "title 1", "blog post 1"),
    PostData(PostId("2"), "title 2", "blog post 2"),
    PostData(PostId("3"), "title 3", "blog post 3"),
    PostData(PostId("4"), "title 4", "blog post 4"),
    PostData(PostId("5"), "title 5", "blog post 5")
  )

  override def list(): Future[Iterable[PostData]] = {
    Future.successful {
      logger.trace(s"list: ")
      postList
    }
  }

  override def get(id: PostId): Future[Option[PostData]] = {
    Future.successful {
      logger.trace(s"get: id = $id")
      postList.find(post => post.id == id)
    }
  }

  def create(data: PostData): Future[PostId] = {
    Future.successful {
      logger.trace(s"create: data = $data")
      data.id
    }
  }

}
