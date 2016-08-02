import play.api.i18n.Messages
import play.api.mvc.Result

package object post {
  implicit def requestToMessages[A](implicit r: PostRequest[A]): Messages = {
    r.messages
  }

  /**
   * Useful Result enrichment to add new methods onto the Result.
   */
  implicit class PostResult(result: Result) {

    def withFlashSuccess(message: String): Result = {
      result.flashing("success" -> message)
    }

    def withFlashError(message: String): Result = {
      result.flashing("error" -> message)
    }

  }

}