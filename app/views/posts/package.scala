package views.html // must be to the package name

import play.api.i18n.Messages
import post.PostRequest

package object posts {
  implicit def requestToMessages[A](implicit r: PostRequest[A]): Messages = {
    r.messages
  }
}
