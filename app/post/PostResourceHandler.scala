package post

import javax.inject.Inject

import circuitbreaker._
import com.lightbend.blog.comment._
import com.lightbend.blog.post._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Controls access to the repositories, returning [[PostResource]] and [[CommentResource]].
 */
class PostResourceHandler @Inject()(postRepository: PostRepository,
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
        PostResource(data, Seq.empty)
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
          maybePostData.map(PostResource(_, comments))
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
            PostResource(p, comments)
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

}
