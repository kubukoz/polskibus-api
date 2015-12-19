package com.kubukoz.polskibus

import com.kubukoz.polskibus.providers.{MockRoutesProvider, RealRoutesProvider}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class RoutesProviderTests extends FlatSpec with Matchers{
  "MockRoutesProvider.getRoutes" should "return 50 cities" in {
    Await.result(MockRoutesProvider.getRoutes, 5.seconds).size should be === 50
  }

  "RealRoutesProvider.getRoutes" should "not throw any exceptions" in {
    Await.result(RealRoutesProvider.getRoutes, 5.seconds).size should be > 0
  }

}