package com.kubukoz.polskibus

import akka.actor.ActorSystem

object ActorsFun {
  def main(args: Array[String]) {
    implicit val system = ActorSystem()
    val myActor = system.actorOf(MyActor.props)

    myActor ! "test"
    myActor ! "dupa"
    myActor ! 5
  }
}

import akka.actor.{Actor, Props}
import akka.event.Logging

class MyActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case "test" => log.info("received test")
    case _:String => context become happy
  }

  def happy: Receive = {
    case _ => log.info("yolololo")
  }
}

object MyActor {
  def props = Props[MyActor]
}