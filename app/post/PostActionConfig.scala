package post

import com.typesafe.config.Config

import java.time.Duration

/**
 * Stores configuration settings related to the PostAction.
 *
 * @param maxAge the maximum age the response should be cached for.
 */
final case class PostActionConfig(maxAge: Duration, timeout: Duration)

object PostActionConfig {
  def fromConfiguration(config: Config): PostActionConfig = {
    val maxAge = config.getDuration("restapi.postAction.maxAge")
    val timeout = config.getDuration("restapi.postAction.timeout")
    PostActionConfig(maxAge, timeout)
  }
}
