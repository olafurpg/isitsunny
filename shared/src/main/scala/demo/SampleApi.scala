package demo

import scala.concurrent.Future

case class Weather(iconSvg: String, kind: String, location: Location)
case class Location(lon: String, lat: String, city: String, country: String)
case class WorldCity(country: String,
                     city: String,
                     accentCity: String,
                     region: String,
                     lat: String,
                     lon: String) {

  def toLocation = Location(lon, lat, accentCity, country)
  val cityCountry = s"$city, $country"
  val display = s"$accentCity, $country"
  def matches(search: String): Boolean = {
    cityCountry.startsWith(search)
  }
}

trait SampleApi {
  def echo(name: String): String
  def weatherplz(location: Option[Location]): Option[Weather]
  def cities(input: String): Seq[WorldCity]
}
