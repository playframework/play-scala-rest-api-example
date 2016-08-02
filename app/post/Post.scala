package post

import com.lightbend.blog.post.{PostData, PostId}
import play.api.libs.json._


/**
 * DTO for displaying Comment information.
 */
case class Comment(body: String)

/**
 * DTO for displaying Post information.
 */
case class Post(id: String, link: String, title: String, body: String, comments: Seq[Comment])

object Comment {
  implicit val implicitWrites = new Writes[Comment] {
    def writes(comment: Comment): JsValue = {
      Json.obj(
        "body" -> comment.body
      )
    }
  }
}

object Post {

  implicit val implicitWrites = new Writes[Post] {
    def writes(post: Post): JsValue = {
      Json.obj(
        "id" -> post.id,
        "link" -> post.link,
        "title" -> post.title,
        "body" -> post.body,
        "comments" -> Json.toJson(post.comments)
      )
    }
  }

  def apply(p: PostData, comments: Seq[Comment]): Post = {
    Post(p.id.toString, link(p.id), p.title, p.body, comments)
  }

  def link(id: PostId): String = {
    import com.netaporter.uri.dsl._
    val url = "/posts" / id.toString
    url.toString()
  }

}
