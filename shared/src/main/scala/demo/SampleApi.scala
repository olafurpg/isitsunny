package demo

import scala.concurrent.Future

case class Weather(iconSvg: String, kind: String, location: Location)
case class Location(lon: String, lat: String, city: String, country: String)

trait SampleApi {
  def echo(name: String): String
  def weatherplz(): Option[Weather]
}