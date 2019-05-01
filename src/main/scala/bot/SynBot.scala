package bot

import acronym._
import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.clients.FutureSttpClient
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}
import cats.instances.future._
import com.bot4s.telegram.methods.ParseMode

import scala.concurrent.Future

class SynBot(token: String) extends TelegramBot with Polling with Commands[Future] {
  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.TRACE

  implicit val backend = OkHttpFutureBackend()
  override val client: RequestHandler[Future] = new FutureSttpClient(token)
  val thesaurus = new Thesaurus[Future]

  onCommand("/syn") { msg =>
    msg.text.flatMap(extractArgument).map(thesaurus.lookup) match {
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
                s"As '_${context}_': ${synonyms.take(5).mkString("\n- ", "\n- ", "\n")}"
            }.mkString(s"Synonyms of word '*${word.term}*':\n", "\n", "")

            reply(response, Some(ParseMode.Markdown))(msg).map(_ => ())
        }
    }
  }

  onCommand("/ant") { msg =>
    msg.text.flatMap(extractArgument).map(thesaurus.lookup) match {
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
              case WordMeaning(context, _, antonyms) =>
                s"As '_${context}_': ${antonyms.take(5).mkString("\n- ", "\n- ", "\n")}"
            }.mkString(s"Antonyms of word '*${word.term}*':\n", "\n", "")

            reply(response, Some(ParseMode.Markdown))(msg).map(_ => ())
        }
    }
  }

  onCommand("/examples") { msg =>
    msg.text.flatMap(extractArgument).map(thesaurus.lookup) match {
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
            val response = word.usageExamples.take(5).map { sentence =>
                s"_${sentence}_"
            }.mkString(s"Usage of word '*${word.term}*':\n- ", "\n- ", "")

            reply(response, Some(ParseMode.Markdown))(msg).map(_ => ())
        }
    }
  }

  private def extractArgument(text: String): Option[String] = {
    if (text.indexOf(" ") == -1) None
    else Some(text.substring(text.indexOf(" ") + 1))
  }
}
