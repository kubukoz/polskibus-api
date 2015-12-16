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

  def getRoutes: Seq[CityPair] = {
    val partialPairs = getPartialPairs
    val allCities = partialPairs.flatMap(_._2.map(_.lift)).toMap
    getPartialPairs.map { case (id, cities) =>
      CityPair(allCities(id), cities)
    }
  }
}

object MockRoutesProvider extends RoutesProvider {
  override def routeSource: Source = Source.fromFile("routes.xml")

  override protected lazy val fetchRoutes: List[String] = super.fetchRoutes
}

trait CityRepository {
  def routesFor(cityId: CityId): Seq[City]

  def getOrFetchCities: Seq[City]

  def getOrFetchRoutes: Seq[CityPair]
}

object InMemoryCityRepository extends CityRepository {
  val routesProvider = MockRoutesProvider
  var cachedRoutes: Seq[CityPair] = Nil

  override def getOrFetchRoutes: Seq[CityPair] = cachedRoutes match {
    case Nil => routesProvider.getRoutes
    case _ => cachedRoutes
  }

  override def getOrFetchCities: Seq[City] = getOrFetchRoutes.map(_.start)

  override def routesFor(cityId: CityId): Seq[City] =
    getOrFetchRoutes
      .find(_.start.id == cityId)
      .map(_.targets.toList).getOrElse(Nil)
}