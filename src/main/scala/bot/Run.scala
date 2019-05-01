package bot

import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Run extends App {
  val config = ConfigFactory.parseResources("credentials/telegram.conf")
  val token = config.getString("token")
  val bot = new SynBot(token)

  val end = bot.run()
  println("Bot is successfully started.")
  Await.ready(end, Duration.Inf)
}
