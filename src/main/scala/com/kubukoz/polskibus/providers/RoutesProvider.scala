package com.kubukoz.polskibus.providers

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{LocalDate, LocalDateTime}

import com.kubukoz.polskibus.config.ServerConfig._
import com.kubukoz.polskibus.domain._
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.libs.ws.WSClient

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps
import scala.util.Try
import scala.xml.XML
import scalacache.ScalaCache
import scalacache.guava.GuavaCache

trait RoutesProvider {
  def routeSource: Source

  protected def fetchRoutes: Future[List[String]] = Future(routeSource.getLines
    .dropWhile(!_.take(CityPairSearchTreshold).contains(CityPairVarDefinition))
    .take(2).map(_.split(" = ")(1)).toList)

  protected def getPartialPairs = fetchRoutes.map(line => (Json.parse(line.head.drop(1).dropRight(2)): @unchecked) match {
    case JsObject(pairs) =>
      pairs.map { case (id, cities) =>
        (CityId(id.toInt), cities.as[JsArray].value.flatMap(City.fromJson))
      }
    case _ => Nil
  })

  def getRoutes: Future[List[CityPair]] = {
    val partialPairsFuture = getPartialPairs
    val allCitiesFuture = partialPairsFuture.map(_.flatMap(_._2.map(_.lift)).toMap)
    (partialPairsFuture zip allCitiesFuture).map { case (pairs, cities) =>
      pairs.map { case (id, targets) =>
        CityPair(cities(id), targets)
      }.toList
    }
  }
}

trait PassageProvider {
  def getPassagesRaw(from: CityId, to: CityId,
                     dateStart: LocalDate, dateEnd: Option[LocalDate] = None,
                     adults: Int, lang: String)(implicit ws: WSClient): Future[String]

  def getPassages(from: CityId, to: CityId,
                  dateStart: LocalDate, dateEnd: Option[LocalDate] = None,
                  adults: Int = 1, lang: String = "PL")(implicit ws: WSClient): Future[Seq[PassageInfo]] = {
    getPassagesRaw(from, to, dateStart, dateEnd, adults, lang)
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
          val info = List("two", "four", "five", "six").map { num =>
            num -> row.find(prt => (prt \ "@class").text contains "onb_" + num)
          }.toMap


          //dates (onb_two)
          val pattern = "Odjazd(.+)\\-(.+)Przyjazd(.+)\\-(.+)".r
          val Some((fromHour, fromDate, toHour, toDate)) = info("two").flatMap {
            nod =>
              val dates = (nod \ "p" \ "strong").text.split("\n| ").mkString("")
              dates match {
                case pattern(a, b, c, d) => Some(a, b, c, d)
                case _ => None
              }
          }
          val datePattern = "dd.MM.yyyy HH:mm"

          val (dateTimeStart, dateTimeEnd) = (
            LocalDateTime.parse(fromDate + " " + fromHour, DateTimeFormatter.ofPattern(datePattern)),
            LocalDateTime.parse(toDate + " " + toHour, DateTimeFormatter.ofPattern(datePattern))
            )

          val (diffHours, diffMinutes) = (
            ChronoUnit.HOURS.between(dateTimeStart, dateTimeEnd).toInt,
            ChronoUnit.MINUTES.between(dateTimeStart, dateTimeEnd) % 60 toInt
            )

          //line type (onb_four)
          val Some((lineName, speed)) = info("four").map { nod =>
            val stuff = nod \ "p"
            stuff.text.split("\n| ").mkString("").partition(s => s == 'P' || Try(s.toString.toInt).isSuccess)
          }.map { case (l, sp) => (l, BusSpeed(sp)) }

          //bus type (onb_six)
          val Some(busType) = info("six").map { nod =>
            BusType((nod \ "img" \ "@src").text.split("/img/").last.split("-bus").head)
          }

          //price (onb_five)
          val Some(price) = info("five").map { nod =>
            (nod \ "p").last.text.split("\n| |z|ł").mkString("").toFloat
          }

          PassageInfo(dateTimeStart.toString, dateTimeEnd.toString, diffHours, diffMinutes, lineName, speed, busType, price)
        }
      }
  }

}

object RealPassageProvider extends PassageProvider {

  override def getPassagesRaw(from: CityId, to: CityId,
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

  implicit val cache = ScalaCache(GuavaCache())

  import scalacache._
  import memoization._

  override def getPassages(from: CityId, to: CityId, dateStart: LocalDate, dateEnd: Option[LocalDate], adults: Int, lang: String)(implicit ws: WSClient): Future[Seq[PassageInfo]] = {
    memoize(6 hours) {
      super.getPassages(from, to, dateStart, dateEnd, adults, lang)
    }
  }
}

object MockPassageProvider extends PassageProvider{
  override def getPassagesRaw(from: CityId, to: CityId, dateStart: LocalDate, dateEnd: Option[LocalDate], adults: Int, lang: String)(implicit ws: WSClient): Future[String] =
    Future(Source.fromFile("sample_xml/passages.xml").getLines.mkString("\n"))
}

trait CityProvider {
  val routesProvider: RoutesProvider

  def cities: Future[Set[City]] = routesProvider.getRoutes.map(_.flatMap(_.targets).toSet)

  def routesFor(cityId: CityId): Future[List[City]] = routesProvider.getRoutes.map {
    _.find(_.start.id == cityId)
      .map(_.targets.toList).getOrElse(Nil)
  }
}

object InMemoryCityProvider extends CityProvider {
  override val routesProvider = RealRoutesProvider
}