package com.kubukoz.polskibus.providers

import com.kubukoz.polskibus.config.ServerConfig._

import scala.io.Source

object RealRoutesProvider extends RoutesProvider {
  override def routeSource: Source = Source.fromURL(RouteSourceUrl(DefaultLanguage))
}
