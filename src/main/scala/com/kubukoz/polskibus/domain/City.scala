package com.kubukoz.polskibus.domain

import java.time.{LocalDate, LocalDateTime}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import play.api.libs.json.JsValue
import spray.json.DefaultJsonProtocol

import scala.language.postfixOps

case class City(id: CityId, name: String) {
  def lift = id -> this
}

trait CityJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val cityIdFormat = jsonFormat(construct = CityId, fieldName1 = "value")
  implicit val cityFormat = jsonFormat2(City.apply)
  implicit val busTypeFormat = jsonFormat1(BusType.apply)
  implicit val busSpeedFormat = jsonFormat1(BusSpeed.apply)
  implicit val passageInfoFormat = jsonFormat8(PassageInfo.apply)
}

object City {
  def fromJson(jsPair: JsValue): Option[City] = {
    val idOpt = (jsPair \ "First").asOpt[String].map(_.toInt)
    val nameOpt = (jsPair \ "Second").asOpt[String]

    (idOpt, nameOpt).zipped map {
      (id, name) => City(CityId(id), name)
    } headOption
  }
}

case class CityId(value: Int) extends AnyVal

case class CityPair(start: City, targets: Seq[City])

case class BusType(value: String)

object BusType {
  val Standard = BusType("std")
  val Gold = BusType("gld")
}

case class BusSpeed(value: String)

object BusSpeed {
  val Fast = BusSpeed("Fast")
  val Express = BusSpeed("Express")
  val GExpress = BusSpeed("GExpress")
}

case class PassageInfo(dateTimeStart: String, dateTimeEnd: String, diffHours: Int, diffMinutes: Int, lineName: String, speed: BusSpeed, busType: BusType, price: Float)

case class PassageRequest(from: CityId, to: CityId, dateStart: LocalDate, dateEnd: Option[LocalDate] = None, adults: Int, lang: String = "PL")