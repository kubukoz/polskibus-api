package com.kubukoz.polskibus

import com.kubukoz.polskibus.providers.{MockRoutesProvider, RealRoutesProvider}
import org.scalatest.{FlatSpec, Matchers}

class RoutesProviderTests extends FlatSpec with Matchers {
  "MockRoutesProvider.getRoutes" should "return 50 cities" in {
    MockRoutesProvider.getRoutes.size should be === 50
  }

  "RealRoutesProvider.getRoutes" should "not throw any exceptions" in {
    RealRoutesProvider.getRoutes.size should be > 0
  }
}
