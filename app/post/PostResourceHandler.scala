package post

import javax.inject.{Inject, Provider}

import circuitbreaker._
import com.lightbend.blog.comment._
import com.lightbend.blog.post._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Controls access to the repositories, returning [[PostResource]] and [[CommentResource]].
 */
class PostResourceHandler @Inject()(routerProvider: Provider[PostRouter],
                                    postRepository: PostRepository,
                                    commentRepository: CommentRepository,
                                    val failsafeBuilder: FailsafeBuilder)
                                   (implicit ec: ExecutionContext)
  extends CircuitBreaker {

  /**
   * Creates a post in the repository.
   */
  def create[A](postInput: PostFormInput): Future[PostResource] = {
    val data = PostData(PostId("999"), postInput.title, postInput.body)
    breaker.async { _ =>
      // We don't actually create the post, so return what we have
      postRepository.create(data).map { id =>
        createPost(data, Seq.empty)
      }
    }
  }

  /**
   * Looks up a single Post from the repository.
   */
  def lookup[A](id: String): Future[Option[PostResource]] = {
    breaker.async { _ =>
      val postFuture = postRepository.get(PostId(id))
      val commentsFuture = findComments(id)
      postFuture.flatMap { maybePostData =>
        commentsFuture.map { comments =>
          maybePostData.map(createPost(_, comments))
        }
      }
    }
  }

  /**
   * Finds all posts.
   */
  def find: Future[Iterable[PostResource]] = {
    breaker.async { _ =>
      postRepository.list().flatMap { postDataList =>
        // Get an Iterable[Future[Post]] containing comments
        val listOfFutures = postDataList.map { p =>
          findComments(p.id.toString).map { comments =>
            createPost(p, comments)
          }
        }

        // Flip it into a single Future[Iterable[Post]]
        Future.sequence(listOfFutures)
      }
    }
  }

  /**
   * Finds comments in the repository.
   */
  private def findComments(postId: String): Future[Seq[CommentResource]] = {
    breaker.async { _ =>
      // Find all the comments for this post
      commentRepository.findByPost(postId.toString).map { comments =>
        comments.map(c => CommentResource(c.body)).toSeq
      }
    }
  }

  private def createPost(p: PostData, comments: Seq[CommentResource]): PostResource = {
    PostResource(p.id.toString, routerProvider.get.link(p.id), p.title, p.body, comments)
  }

}
