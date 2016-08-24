import play.api.i18n.Messages
import play.api.mvc.Result

/**
 * Package object for post.  This is a good place to put implicit conversions.
 */
package object post {

  /**
   * Converts between PostRequest and Messages automatically.
   */
  implicit def requestToMessages[A](implicit r: PostRequest[A]): Messages = {
    r.messages
  }
}
