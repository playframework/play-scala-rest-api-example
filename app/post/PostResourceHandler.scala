package post

import javax.inject.{Inject, Provider}

import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json._


/**
 * DTO for displaying post information.
 */
case class PostResource(id: String,
                        link: String,
                        title: String,
                        body: String)

object PostResource {

  /**
   * Mapping to write a PostResource out as a JSON value.
   */
  implicit val implicitWrites = new Writes[PostResource] {
    def writes(post: PostResource): JsValue = {
      Json.obj(
        "id" -> post.id,
        "link" -> post.link,
        "title" -> post.title,
        "body" -> post.body
      )
    }
  }
}

/**
 * Controls access to the repositories, returning [[PostResource]]
 */
class PostResourceHandler @Inject()(routerProvider: Provider[PostRouter],
                                    postRepository: PostRepository)
                                   (implicit ec: ExecutionContext)
{

  def create[A](postInput: PostFormInput): Future[PostResource] = {
    val data = PostData(PostId("999"), postInput.title, postInput.body)
    // We don't actually create the post, so return what we have
    postRepository.create(data).map { id =>
      createPostResource(data)
    }
  }

  def lookup[A](id: String): Future[Option[PostResource]] = {
    val postFuture = postRepository.get(PostId(id))
    postFuture.map { maybePostData =>
      maybePostData.map { postData =>
        createPostResource(postData)
      }
    }
  }

  def find: Future[Iterable[PostResource]] = {
    postRepository.list().map { postDataList =>
      postDataList.map(postData => createPostResource(postData))
    }
  }

  private def createPostResource(p: PostData): PostResource = {
    PostResource(p.id.toString, routerProvider.get.link(p.id), p.title, p.body)
  }

}
