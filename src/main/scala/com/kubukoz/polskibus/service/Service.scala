package com.kubukoz.polskibus.service

import java.time.LocalDate

import akka.actor.{Actor, ActorSystem, Props}
import akka.event.LoggingAdapter
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.Directives._
import akka.pattern.{ask, pipe}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.kubukoz.polskibus.config.ServerConfig._
import com.kubukoz.polskibus.domain._
import com.kubukoz.polskibus.domain.messages.{CitiesStartingWith, RoutesForCity}
import com.kubukoz.polskibus.providers.{RealPassageProvider, PassageProvider, CityProvider, InMemoryCityProvider}
import com.typesafe.config.Config
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, ExecutionContextExecutor}
import scala.concurrent.duration._
import scala.language.postfixOps
import scalacache.ScalaCache
import scalacache.guava.GuavaCache

trait Service extends CityJsonSupport {
  implicit val actorSystem: ActorSystem
  implicit val config: Config

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: ActorMaterializer
  implicit val logger: LoggingAdapter

  implicit val routeActorCallTimeout = Timeout(5 seconds)

  def getRoutesForCity(cityId: CityId): ToResponseMarshallable = {
    (routeActor ? RoutesForCity(cityId)).mapTo[List[City]]
  }


  def getCitiesStartingWith(nameStart: String): ToResponseMarshallable = {
    (routeActor ? CitiesStartingWith(nameStart)).mapTo[List[City]]
  }

  def getPassages(from: String, to: String, dateFrom: String, dateTo: Option[String], adults: String): ToResponseMarshallable = {
    (routeActor ? PassageRequest(CityId(from.toInt), CityId(to.toInt), LocalDate.parse(dateFrom), dateTo.map(LocalDate.parse), adults.toInt)).mapTo[List[PassageInfo]]
  }

  lazy val routeActor = actorSystem.actorOf(Props[RouteActor])

  val routes =
    logRequestResult(ActorSystemName) {
      pathPrefix("targets" / IntNumber) { cityId =>
        get {
          complete {
            getRoutesForCity(CityId(cityId))
          }
        }
      } ~ pathPrefix("cities" / "startingWith" / Rest) { nameStart =>
        get {
          complete {
            getCitiesStartingWith(nameStart)
          }
        }
      } ~ pathPrefix("passages") {
        get {
          parameters("from", "to", "dateFrom", "dateTo".?, "adults") { (from, to, dateFrom, dateTo, adults) =>
            complete {
              getPassages(from, to, dateFrom, dateTo, adults)
            }
          }
        }
      }
    }
}

class RouteActor extends AbstractRouteActor {
  override val cityProvider: CityProvider = InMemoryCityProvider
  override val passageProvider: PassageProvider = RealPassageProvider
}

trait AbstractRouteActor extends Actor {
  val cityProvider: CityProvider
  val passageProvider: PassageProvider

  implicit val ws = NingWSClient()

  override def receive: Receive = {
    case RoutesForCity(cityId) =>
      cityProvider.routesFor(cityId) pipeTo sender
    case CitiesStartingWith(namePrefix) =>
      cityProvider.cities.map(_.filter(_.name.toLowerCase.startsWith(namePrefix.toLowerCase))) pipeTo sender
    case PassageRequest(from, to, ds, deOpt, ad, lang) =>
      passageProvider.getPassages(from, to, ds, deOpt, ad, lang).map(_.toList) pipeTo sender
  }
}