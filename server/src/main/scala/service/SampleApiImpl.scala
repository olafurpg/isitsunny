package service

import scala.language.postfixOps

import scala.collection.mutable
import scala.util.Try
import scala.xml.XML

import java.util.Date

import demo.Location
import demo.SampleApi
import demo.Weather
import met.locationforecast.TimeType
import met.locationforecast.Weatherdata
import upickle.Js

import scalaj.http._

/**
  * Created by Janos on 12/9/2015.
  */
class SampleApiImpl(remoteIp: String) extends SampleApi {

  val dbIpKey = "cad0f130df7d4ef533bf4bc20c098bdf7b9e5d09"

  override def echo(name: String): String = s"Echoed: ${name}"
  val bergen = Location(lon = "5.32205",
                        lat = "60.39126",
                        city = "B" + "ergen",
                        country = "Norway")

  def getTime(times: Seq[TimeType]): TimeType = {
    times.minBy(_.to.getMillisecond)
  }

  def getLocation(response: String): Location = {
    Try({
      val obj: Map[String, Js.Value] =
        upickle.json.read(response).asInstanceOf[Js.Obj].value.toMap
      Location(obj("longtitude").str,
               obj("latitude").str,
               obj("city").str,
               obj("country").str)
    }).getOrElse(bergen)
  }

  override def weatherplz(): Option[Weather] = {
    val resp = Http("http://api.db-ip.com/addrinfo")
      .param("api_key", dbIpKey)
      .param("addr", remoteIp)
      .timeout(10000, 10000)
      .asString

    val location = getLocation(resp.body)
    val req2 = Http("http://api.met.no/weatherapi/locationforecast/1.9")
      .param("lat", location.lat)
      .param("lon", location.lon)
      .asString
    println(req2.body)
    val weatherData = scalaxb.fromXML[Weatherdata](XML.loadString(req2.body))
    println(weatherData)
    val now = new Date()
    val bestTime = weatherData.product
      .sortBy(x => getTime(x.time).to.getMillisecond)
      .headOption
    for {
      product <- weatherData.product.headOption
      time <- product.time.find(_.location.exists(
              _.locationtypesequence1.headOption.exists(_.symbol.isDefined)))
      metLocation <- time.location.headOption
      symbol <- metLocation.locationtypesequence1
        .find(_.symbol.isDefined)
        .flatMap(_.symbol)
      kind <- symbol.id
      icon <- WeatherIcons.getIcon(symbol.number.toString)
    } yield Weather(icon, kind, location)
  }
}

object WeatherIcons {
  private val iconCache = mutable.Map.empty[String, String]
  def getIcon(id: String): Option[String] = {
    println(id)
    Try(
        Http("http://api.met.no/weatherapi/weathericon/1.1/")
          .param("symbol", id)
          .param("is_night", "0")
          .param("content_type", "image/svg+xml")
          .asString
          .body
      ).filter(!_.contains("error")).toOption
  }
}
