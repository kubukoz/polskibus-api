package com.kubukoz.polskibus

import java.time.LocalDate

import com.kubukoz.polskibus.domain.CityId
import com.kubukoz.polskibus.providers.{MockPassageProvider, RealPassageProvider}
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.Await
import scala.concurrent.duration._

class PassageProviderTests extends FlatSpec with Matchers {
  implicit val ws = NingWSClient()

  "RealPassageProvider" should "provide any positive amount of passages from 44 to 15" in {
    Await.result(
      RealPassageProvider.getPassages(CityId(44), CityId(15), LocalDate.now().plusDays(2)), 5.seconds)
      .size should be > 0
  }
  "PassageProvider" should "provide 3 passages" in {
    Await.result(
      MockPassageProvider.getPassages(CityId(44), CityId(15), LocalDate.now().plusDays(2)), 5.seconds
    ).size shouldEqual 3
  }
}
