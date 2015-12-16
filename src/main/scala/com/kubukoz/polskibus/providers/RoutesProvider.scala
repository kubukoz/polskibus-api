package com.kubukoz.polskibus.providers

import com.kubukoz.polskibus.config.ServerConfig._
import com.kubukoz.polskibus.domain.{City, CityId, CityPair}
import play.api.libs.json.{JsArray, JsObject, Json}

import scala.io.Source

trait RoutesProvider {
  def routeSource: Source

  protected def fetchRoutes: List[String] = routeSource.getLines
    .dropWhile(!_.take(CityPairSearchTreshold).contains(CityPairVarDefinition))
    .take(2).map(_.split(" = ")(1)).toList

  protected def getPartialPairs = (Json.parse(fetchRoutes.head.drop(1).dropRight(2)): @unchecked) match {
    case JsObject(pairs) =>
      pairs.map { case (id, cities) =>
        (CityId(id.toInt), cities.as[JsArray].value.flatMap(City.fromJson))
      }
    case _ => Nil
  }

  def getRoutes = {
    val partialPairs = getPartialPairs
    val allCities = partialPairs.flatMap(_._2.map(_.lift)).toMap
    getPartialPairs.map { case (id, cities) =>
      CityPair(allCities(id), cities)
    }
  }
}