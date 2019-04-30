package bot

import com.typesafe.config.ConfigFactory

object Run extends App {
  val config = ConfigFactory.parseResources("credentials/telegram.conf")
  val token = config.getString("token")
  val bot = new Bot(token)

  bot.run()
  println("Bot is successfully started.")
}
