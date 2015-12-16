package com.kubukoz.polskibus.config

object ServerConfig {
  val ActorSystemName = "polskibus-backend"

  val CityPairSearchTreshold = 20
  val CityPairVarName = "cityPair"
  val CityPairVarDefinition = s"var $CityPairVarName"

  val DefaultLanguage = "PL"

  val RouteSourceUrl = (lang: String) => s"http://booking.polskibus.com/pricing/selections?lang=$lang"
}