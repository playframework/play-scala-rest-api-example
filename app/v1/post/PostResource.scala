package v1.post

import play.api.libs.json.Json

/**
  * DTO for displaying post information.
  */
case class PostResource(id: String, link: String, title: String, body: String)

object PostResource {
  implicit val formatter = Json.format[PostResource]
}
