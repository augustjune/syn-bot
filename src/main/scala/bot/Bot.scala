package bot

import acronym.{JsonParsingError, Misspelling, NoWordProvided, ServerError, Thesaurus, WordMeaning}
import com.bot4s.telegram.api.declarative.{Commands, Messages}
import com.bot4s.telegram.api.{Polling, RequestHandler, TelegramBot}
import com.bot4s.telegram.clients.SttpClient
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}
import cats.instances.future._

import scala.concurrent.Future

class Bot(token: String) extends TelegramBot with Messages with Commands with Polling {
  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.INFO

  implicit val backend = OkHttpFutureBackend()
  val client: RequestHandler = new SttpClient(token)
  val thesaurus = new Thesaurus[Future]()

  onCommand("/syn") { msg =>
    msg.text.map(thesaurus.lookup) match {
      case None => ()
      case Some(futureSyn) =>
        futureSyn.flatMap {
          case Left(lookupError) =>
            val response = "Error occurred while handling your request:\n" + (lookupError match {
              case NoWordProvided => "No word was provided"
              case Misspelling(misspelledTerm) => s"Did you mean: $misspelledTerm"
              case JsonParsingError(message) => "We could not parse the response from server."
              case ServerError(message) => "We could not connect to server."
            })

            reply(response)(msg)

          case Right(word) =>
            val response = word.meanings.map {
              case WordMeaning(context, synonyms, _) =>
                s"As $context: ${synonyms.take(5).mkString("", ", ", ".")}"
            }.mkString(s"Synonyms for word ${word.term}:\n", "\n", "")

            reply(response)(msg)
        }
    }
  }

  private def validateMessage(text: String): Option[String] = {
    if (text.isEmpty) None
    else Some(text)
  }
}
