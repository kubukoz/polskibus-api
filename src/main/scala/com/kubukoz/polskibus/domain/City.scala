package com.kubukoz.polskibus.domain

import java.time.LocalDateTime

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

case class RouteDateLimits(from: CityId, to: CityId, firstDate: LocalDateTime, lastDate: LocalDateTime)
