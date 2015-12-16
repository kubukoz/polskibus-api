import java.time.LocalDateTime

import ServerConfig._
import play.api.libs.json.JsValue

import scala.io.Source

object Yolo {
  def main(args: Array[String]): Unit = {
    val routesProvider = new MockRoutesProvider


    routesProvider.getRoutes foreach println
  }
}

object ServerConfig {
  val CityPairSearchTreshold = 20
  val CityPairVarName = "cityPair"
  val CityPairVarDefinition = s"var $CityPairVarName"
}

class MockRoutesProvider {
  def getStuff = {
    val z = Source.fromFile("routes.xml").getLines
      .dropWhile(!_.take(CityPairSearchTreshold)
        .contains(CityPairVarDefinition)).take(2).map(_.split(" = ")(1)).toList
    println(s"downloaded $z\n")
    z
  }

  import play.api.libs.json._

  def getRoutes = (Json.parse(getStuff.head.drop(1).dropRight(2)): @unchecked) match {
    case JsObject(pairs) => pairs.map { case (id, cities) =>
      CityPair(CityId(id.toInt), cities.as[JsArray].value.flatMap(City.from))
    case _ => Nil
    }
  }

}

case class CityId(id: Int) extends AnyVal

case class City(id: CityId, name: String)

object City {
  def from(jsPair: JsValue): Option[City] =
    ((jsPair \ "First").asOpt[String].map(_.toInt) zip (jsPair \ "Second").asOpt[String]).headOption
      .map { case (id, name) => City(CityId(id), name) }
}

case class CityPair(id: CityId, targets: Seq[City])

case class RouteDateLimits(from: CityId, to: CityId, firstDate: LocalDateTime, lastDate: LocalDateTime)