package com.kubukoz.polskibus.providers

import com.kubukoz.polskibus.config.ServerConfig._
import com.kubukoz.polskibus.domain.{City, CityId, CityPair}
import play.api.libs.json.{JsArray, JsObject, Json}

import scala.concurrent.Future
import scala.io.Source
import scala.concurrent.ExecutionContext.Implicits.global

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
  def passageSource: Source

  //  protected def fetchPassages: List[String] = passageSource.getLines
}

object MockPassageProvider extends PassageProvider {
  override def passageSource: Source = Source.fromFile("passages.xml")
}

object MockRoutesProvider extends RoutesProvider {
  override def routeSource: Source = Source.fromFile("routes.xml")

  override protected lazy val fetchRoutes: Future[List[String]] = super.fetchRoutes
}

trait CityRepository {
  def routesFor(cityId: CityId): Future[List[City]]

  def getOrFetchCities: Future[List[City]]

  def getOrFetchRoutes: Future[List[CityPair]]
}

object InMemoryCityRepository extends CityRepository {
  val routesProvider = MockRoutesProvider
  var cachedRoutes: List[CityPair] = Nil

  override def getOrFetchRoutes: Future[List[CityPair]] = cachedRoutes match {
    case Nil => routesProvider.getRoutes
    case _ => Future.successful(cachedRoutes)
  }

  override def getOrFetchCities: Future[List[City]] = getOrFetchRoutes.map(_.map(_.start))

  override def routesFor(cityId: CityId): Future[List[City]] =
    getOrFetchRoutes.map {
      _.find(_.start.id == cityId)
        .map(_.targets.toList).getOrElse(Nil)
    }

}