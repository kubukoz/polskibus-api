package com.kubukoz.polskibus

import java.time.LocalDate

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.kubukoz.polskibus.config.ServerConfig._
import com.kubukoz.polskibus.domain.CityId
import com.kubukoz.polskibus.service.Service
import com.typesafe.config.ConfigFactory
import play.api.libs.ws.ning.NingWSClient
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.Future
import scala.io.Source
import scala.language.postfixOps
import scala.reflect.io.File
import scala.xml.XML

object Main extends Service {
  override implicit val actorSystem = ActorSystem(ActorSystemName)
  override implicit val executor = actorSystem.dispatcher
  override implicit val materializer = ActorMaterializer()
  override val config = ConfigFactory.load()
  override val logger = Logging(actorSystem, getClass)

  def main(args: Array[String]): Unit = {
    Http(actorSystem).bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
  }
}

import scala.concurrent.ExecutionContext.Implicits.global

object Demo {

  def getPassagesRaw(from: CityId, to: CityId,
                     dateStart: LocalDate, dateEnd: Option[LocalDate] = None,
                     adults: Int, lang: String)(implicit ws: WSClient): Future[String] = {
    val PolskiBusHomePage = "http://booking.polskibus.com/pricing/selections?lang=PL"
    ws.url(PolskiBusHomePage).get.flatMap { res =>
      val sesId = res.cookie("ASP.NET_SessionId").flatMap(_.value).get
      val bodyLines = res.body.split("\n")

      val fieldDef = "id=\"__VIEWSTATE\""

      val viewStatePattern = (fieldDef + " value=\"(.+)\"").r
      val viewState = bodyLines.find(_.contains(fieldDef)).flatMap(viewStatePattern.findFirstIn)

      val generatorFieldDef = "name=\"__VIEWSTATEGENERATOR\""
      val viewStateGeneratorPattern = (generatorFieldDef + " value=\"(.+)\"").r

      val viewStateGenerator = bodyLines.find(_.contains(generatorFieldDef)).flatMap(viewStateGeneratorPattern.findFirstIn)
      def dateToString(date: LocalDate) = s"${date.getDayOfMonth}/${date.getMonthValue}/${date.getYear}"

      val dateStartString = dateToString(dateStart)
      val dateEndString = dateEnd.map(dateToString).getOrElse("")

      val query = List("__VIEWSTATE" -> viewState.getOrElse(""),
        "PricingForm.DBType" -> "MY",
        "PricingForm.hidSessionId" -> sesId,
        "PricingForm.hidLang" -> lang,
        "PricingForm.hidPC" -> "",
        "PricingForm.Adults" -> adults.toString,
        "PricingForm.FromCity" -> from.value.toString,
        "PricingForm.ToCity" -> to.value.toString,
        "PricingForm.OutDate" -> dateStartString,
        "__VIEWSTATEGENERATOR" -> viewStateGenerator.getOrElse(""),
        "PricingForm.RetDate" -> dateEndString,
        "PricingForm.PromoCode" -> "",
        "PricingForm.ConcessionCode" -> "")

      val headers = List("Content-Type" -> "application/x-www-form-urlencoded", "Content-Length" -> "0")

      ws.url("http://booking.polskibus.com/Pricing/GetPrice")
        .withHeaders(headers: _*)
        .withQueryString(query: _*).execute("POST").map(_.body)
    }
  }

  def getPassagesRawFromFile(from: CityId, to: CityId,
                             dateStart: LocalDate, dateEnd: Option[LocalDate] = None,
                             adults: Int, lang: String)(implicit ws: WSClient): Future[String] = Future(Source.fromFile("bookings.xml").getLines mkString "\n")


  def getPassages(from: CityId, to: CityId,
                  dateStart: LocalDate, dateEnd: Option[LocalDate] = None,
                  adults: Int = 1, lang: String = "PL")(implicit ws: WSClient) = {
    getPassagesRawFromFile(from, to, dateStart, dateEnd, adults, lang)
      .map { res =>
        val results = XML.loadString(res.split("<form(.+)>").last.split("</form>").head.replaceAll("(&nbsp;)|(<br>)", " "))
        val rows = for {
          div <- results \ "div"
          if (div \ "@class").text == "onb_resultRow"
        } yield for {
          part <- div \\ "div"
          if (part \ "@class").text contains "onb_col"
        } yield part

        rows.map { row =>
          val info = List("two", "three", "four", "five", "six").map { num =>
            num -> row.find(prt => (prt \ "@class").text contains "onb_" + num)
          }.toMap

          val pattern = "Odjazd(.+)\\-(.+)Przyjazd(.+)\\-(.+)".r
          val Some((fromHour, fromDate, toHour, toDate)) = info("two").flatMap {
            nod =>
              val dates = (nod \ "p" \ "strong").text.split("\n| ").mkString("")
              dates match {
                case pattern(a, b, c, d) => Some(a, b, c, d)
                case _ => None
              }
          }
          (fromHour, fromDate, toHour, toDate) //we deserve this data
        } foreach println
      }
  }

  def main(args: Array[String]) {
    implicit val ws = NingWSClient()
    getPassages(CityId(44), CityId(17), LocalDate.of(2015, 12, 20), lang = "PL")
  }
}


























