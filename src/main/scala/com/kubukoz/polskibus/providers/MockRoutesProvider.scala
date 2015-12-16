package com.kubukoz.polskibus.providers

import scala.io.Source

object MockRoutesProvider extends RoutesProvider {
  override def routeSource: Source = Source.fromFile("routes.xml")

  override protected lazy val fetchRoutes: List[String] = super.fetchRoutes
}
