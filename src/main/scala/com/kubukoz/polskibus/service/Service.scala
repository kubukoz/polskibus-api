package com.kubukoz.polskibus.service

import akka.actor.{Actor, ActorSystem, Props}
import akka.event.LoggingAdapter
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.kubukoz.polskibus.config.ServerConfig._
import com.kubukoz.polskibus.domain.{CityId, City, CityJsonSupport}
import com.kubukoz.polskibus.providers.{InMemoryCityRepository, CityRepository, MockRoutesProvider, RoutesProvider}
import com.typesafe.config.Config

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.language.postfixOps

trait Service extends CityJsonSupport {
  implicit val actorSystem: ActorSystem
  implicit val config: Config

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: ActorMaterializer
  implicit val logger: LoggingAdapter

  def getRoutesForCity(cityId: CityId): ToResponseMarshallable = {
    implicit val timeout = Timeout(1 second)
    (routeActor ? cityId).mapTo[List[City]]
  }

  lazy val routeActor = actorSystem.actorOf(Props[RouteActor])

  def getCitiesStartingWith(nameStart: String) = ???

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
    case cityId: CityId =>
      sender ! cityRepository.routesFor(cityId)
  }
}