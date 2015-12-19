package com.kubukoz.polskibus

import com.kubukoz.polskibus.domain.CityId
import com.kubukoz.polskibus.providers.{CityProvider, MockRoutesProvider, RoutesProvider}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class CityProviderTests extends FlatSpec with Matchers with CityProvider{
  override val routesProvider: RoutesProvider = MockRoutesProvider
  "CityProvider.cities" should "return 50 cities" in{
    Await.result(cities, 5.seconds).size shouldEqual 50
  }
  "CityProvider.routesFor(44)" should "return 46 routes" in{
    Await.result(routesFor(CityId(44)), 5.seconds).size shouldEqual 46
  }
}