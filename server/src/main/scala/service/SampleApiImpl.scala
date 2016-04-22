package service

import scala.language.postfixOps

import scala.collection.mutable
import scala.util.Try
import scala.xml.XML

import java.nio.file.Files
import java.nio.file.Path
import java.util.Date

import com.google.common.io.ByteStreams
import demo.Location
import demo.SampleApi
import demo.Weather
import demo.WorldCity
import met.locationforecast.TimeType
import met.locationforecast.Weatherdata
import upickle.Js

import scalaj.http._

object WorldCities {
  lazy val contents: String = {
    new String(
        ByteStreams.toByteArray(this
              .getClass()
              .getClassLoader()
              .getResourceAsStream("worldcitiespop.txt")))
  }
  lazy val cities: Seq[WorldCity] = {
    contents.lines
      .drop(1)
      .map { row =>
        Try({
          val columns = row.split(",")
          WorldCity(
              columns(0),
              columns(1),
              columns(2),
              columns(3),
              columns(5),
              columns(6)
          )
        }).toOption
      }
      .toSeq
      .collect {
        case Some(city) => city
      }
  }
}

/**
  * Created by Janos on 12/9/2015.
  */
class SampleApiImpl(remoteIp: String) extends SampleApi {

  val dbIpKey = "cad0f130df7d4ef533bf4bc20c098bdf7b9e5d09"

  override def echo(name: String): String = s"Echoed: ${name}"
  val bergen = Location(lon = "5.32205",
                        lat = "60.39126",
                        city = "Bergen",
                        country = "Norway")

  def getTime(times: Seq[TimeType]): TimeType = {
    times.minBy(_.to.getMillisecond)
  }

  def getLocation(): Location = {
    Try({
      val response = Http("http://api.db-ip.com/addrinfo")
        .param("api_key", dbIpKey)
        .param("addr", remoteIp)
        .asString
      println(s"db info: ${response.body}")
      val obj: Map[String, Js.Value] =
        upickle.json.read(response.body).asInstanceOf[Js.Obj].value.toMap
      println(obj)
      obj
        .get("longitude")
        .map { _ =>
          Location(obj("longitude").str,
                   obj("latitude").str,
                   obj("city").str,
                   obj("country").str)
        }
        .getOrElse {
          cities(obj("city").str).head.toLocation
        }
    }).recover({
        case e: Throwable =>
          println(e.getMessage)
          throw e
      })
      .getOrElse(bergen)
  }

  override def weatherplz(customLocation: Option[Location]): Option[Weather] = {
    println(remoteIp)
    println(s"customLocation: $customLocation")
    val location = customLocation.getOrElse {
      getLocation()
    }
    val req2 = Http("http://api.met.no/weatherapi/locationforecast/1.9")
      .param("lat", location.lat)
      .param("lon", location.lon)
      .asString
    val weatherData = scalaxb.fromXML[Weatherdata](XML.loadString(req2.body))
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

  override def cities(input: String): Seq[WorldCity] = {
    val search = input.toLowerCase.replaceAll("[^a-z ,].*", "")
    WorldCities.cities.toIterator.filter(_.matches(search)).take(10).toSeq
  }
}

object WeatherIcons {
  private val iconCache = mutable.Map.empty[String, String]
  def getIcon(id: String): Option[String] = {
    iconCache.get(id) match {
      case x @ Some(icon) => x
      case _ =>
        Try(
            Http("http://api.met.no/weatherapi/weathericon/1.1/")
              .param("symbol", id)
              .param("is_night", "0")
              .param("content_type", "image/svg+xml")
              .asString
              .body
          )
          .filter(!_.contains("error"))
          .map { icon =>
            iconCache.update(id, icon)
            icon
          }
          .toOption
    }
  }
}
