package bot

import acronym._
import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.clients.FutureSttpClient
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}
import cats.instances.future._

import scala.concurrent.Future

class Bot(token: String) extends TelegramBot with Polling with Commands[Future] {
  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.INFO

  implicit val backend = OkHttpFutureBackend()
  override val client: RequestHandler[Future] = new FutureSttpClient(token)
  val thesaurus = new Thesaurus[Future]

  onCommand("/syn") { msg =>
    msg.text.flatMap(validateMessage).map(thesaurus.lookup) match {
      case None => Future.unit
      case Some(futureSyn) =>
        futureSyn.flatMap {
          case Left(lookupError) =>
            val response = "Error occurred while handling your request:\n" + (lookupError match {
              case NoWordProvided => "No word was provided"
              case Misspelling(misspelledTerm) => s"Did you mean: $misspelledTerm"
              case JsonParsingError(message) => s"We could not parse the response from server.\n\t$message"
              case ServerError(message) => s"We could not connect to server.\n\t$message"
            })

            reply(response)(msg).map(_ => ())

          case Right(word) =>
            val response = word.meanings.map {
              case WordMeaning(context, synonyms, _) =>
                s"As '$context': ${synonyms.take(5).mkString("", ", ", ".")}"
            }.mkString(s"Synonyms for word '${word.term}':\n", "\n", "")

            reply(response)(msg).map(_ => ())
        }
    }
  }

  private def validateMessage(text: String): Option[String] = {
    if (text.indexOf(" ") == -1) None
    else Some(text.substring(text.indexOf(" ") + 1))
  }
}
