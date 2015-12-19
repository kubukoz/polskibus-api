package com.kubukoz.polskibus

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.kubukoz.polskibus.config.ServerConfig._
import com.kubukoz.polskibus.service.Service
import com.typesafe.config.ConfigFactory

import scala.language.postfixOps

object Main extends Service {
  override implicit val actorSystem = ActorSystem(ActorSystemName)
  override implicit val executor = actorSystem.dispatcher
  override implicit val materializer = ActorMaterializer()
  override val config = ConfigFactory.load()
  override val logger = Logging(actorSystem, getClass)

  def main(args: Array[String]): Unit = {
    Http(actorSystem).bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
  }
}