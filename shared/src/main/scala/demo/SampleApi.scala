package demo

case class Weather(name: String)

trait SampleApi {
  def echo(name: String): String
  def weatherplz(): Weather
}