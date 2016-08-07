package post

import play.api.libs.json._


/**
 * DTO for displaying comment information.
 */
case class CommentResource(body: String)


object CommentResource {

  /**
   * Mapping to write a CommentResource out as a JSON value.
   */
  implicit val implicitWrites = new Writes[CommentResource] {
    def writes(comment: CommentResource): JsValue = {
      Json.obj(
        "body" -> comment.body
      )
    }
  }
}

/**
 * DTO for displaying post information.
 */
case class PostResource(id: String,
                        link: String,
                        title: String,
                        body: String,
                        comments: Seq[CommentResource])

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
        "body" -> post.body,
        "comments" -> Json.toJson(post.comments)
      )
    }
  }

}
