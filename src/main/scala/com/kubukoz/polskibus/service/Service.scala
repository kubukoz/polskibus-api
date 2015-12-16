package com.kubukoz.polskibus.service

import akka.actor.{Actor, ActorSystem, Props}
import akka.event.LoggingAdapter
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.kubukoz.polskibus.config.ServerConfig._
import com.kubukoz.polskibus.domain.{City, CityJsonSupport}
import com.kubukoz.polskibus.providers.{MockRoutesProvider, RoutesProvider}
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

  def getRoutesForCity(name: String): ToResponseMarshallable = {
    implicit val timeout = Timeout(1 second)
    (routeActor ? name).mapTo[List[City]]
  }

  lazy val routeActor = actorSystem.actorOf(Props[RouteActor])

  def getCitiesStartingWith(nameStart: String) = ???

  val routes =
    logRequestResult(ActorSystemName) {
      pathPrefix("targets" / Rest) { city =>
        get {
          println(city)
          complete {
            getRoutesForCity(city)
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
  override val routesProvider = MockRoutesProvider
}

trait AbstractRouteActor extends Actor {
  val routesProvider: RoutesProvider

  override def receive: Receive = {
    case cityName: String =>
      sender ! routesProvider.getRoutes.find(_.start.name == cityName).map(_.targets.toList).getOrElse(Nil)
  }
}