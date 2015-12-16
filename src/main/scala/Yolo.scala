import java.time.LocalDateTime

import ServerConfig._
import play.api.libs.json.{JsArray, JsObject, Json, JsValue}

import scala.io.Source

object Yolo {
  def main(args: Array[String]): Unit = {
    val routesProvider: RoutesProvider = new MockRoutesProvider

    routesProvider.getRoutes foreach { case CityPair(city, targets) =>
      println(s"${city.name}:\n" + targets.map("\t\t" + _.name).mkString("\n"))
    }
  }
}

object ServerConfig {
  val CityPairSearchTreshold = 20
  val CityPairVarName = "cityPair"
  val CityPairVarDefinition = s"var $CityPairVarName"

  val RouteSourceUrl = "http://booking.polskibus.com/pricing/selections?lang=PL"
}

trait RoutesProvider {
  def routeSource: Source

  protected def getStuff: List[String] = routeSource.getLines
    .dropWhile(!_.take(CityPairSearchTreshold).contains(CityPairVarDefinition))
    .take(2).map(_.split(" = ")(1)).toList

  protected def getPartialPairs = (Json.parse(getStuff.head.drop(1).dropRight(2)): @unchecked) match {
    case JsObject(pairs) =>
      pairs.map { case (id, cities) =>
        (CityId(id.toInt), cities.as[JsArray].value.flatMap(City.from))
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

class RealRoutesProvider extends RoutesProvider {
  override def routeSource: Source = Source.fromURL(RouteSourceUrl)
}

class MockRoutesProvider extends RoutesProvider {
  override def routeSource: Source = Source.fromFile("routes.xml")
}

case class CityId(id: Int) extends AnyVal

case class City(id: CityId, name: String) {
  def lift = id -> this
}

object City {
  def from(jsPair: JsValue): Option[City] =
    ((jsPair \ "First").asOpt[String].map(_.toInt) zip (jsPair \ "Second").asOpt[String])
      .map { case (id, name) => City(CityId(id), name) }.headOption
}

case class CityPair(start: City, targets: Seq[City])

case class RouteDateLimits(from: CityId, to: CityId, firstDate: LocalDateTime, lastDate: LocalDateTime)