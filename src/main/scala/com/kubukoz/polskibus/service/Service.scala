package com.kubukoz.polskibus.service

import akka.actor.{Actor, ActorSystem, Props}
import akka.event.LoggingAdapter
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.Directives._
import akka.pattern.{ask, pipe}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.kubukoz.polskibus.config.ServerConfig._
import com.kubukoz.polskibus.domain.messages.{CitiesStartingWith, RoutesForCity}
import com.kubukoz.polskibus.domain.{City, CityId, CityJsonSupport}
import com.kubukoz.polskibus.providers.{CityRepository, InMemoryCityRepository}
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.postfixOps

trait Service extends CityJsonSupport {
  implicit val actorSystem: ActorSystem
  implicit val config: Config

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: ActorMaterializer
  implicit val logger: LoggingAdapter

  implicit val routeActorCallTimeout = Timeout(1 second)

  def getRoutesForCity(cityId: CityId): ToResponseMarshallable = {
    (routeActor ? RoutesForCity(cityId)).mapTo[List[City]]
  }

  def getCitiesStartingWith(nameStart: String): ToResponseMarshallable = {
    (routeActor ? CitiesStartingWith(nameStart)).mapTo[List[City]]
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
        complete {
          getCitiesStartingWith(nameStart)
        }
      }
    }
}

class RouteActor extends AbstractRouteActor {
  override val cityRepository: CityRepository = InMemoryCityRepository
}

trait AbstractRouteActor extends Actor {
  val cityRepository: CityRepository

  override def receive: Receive = {
    case RoutesForCity(cityId) =>
      cityRepository.routesFor(cityId) pipeTo sender
    case CitiesStartingWith(namePrefix) =>
      cityRepository.getOrFetchCities.map(_.filter(_.name.toLowerCase.startsWith(namePrefix.toLowerCase))) pipeTo sender
  }
}