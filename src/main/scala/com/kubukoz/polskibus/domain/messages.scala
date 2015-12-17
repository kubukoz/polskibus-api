package com.kubukoz.polskibus.domain

package messages {

case class CitiesStartingWith(nameStart: String)
case class RoutesForCity(cityId: CityId)
}
