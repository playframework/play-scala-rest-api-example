package post

import com.typesafe.config.Config

import java.time.Duration

/**
 * Stores configuration settings related to the PostAction.
 *
 * @param maxAge the maximum age the response should be cached for.
 */
final case class PostActionConfig(maxAge: Duration)

object PostActionConfig {
  def fromConfiguration(config: Config): PostActionConfig = {
    val d = config.getDuration("restapi.postAction.maxAge")
    PostActionConfig(d)
  }
}
