package pages

import scala.concurrent.Future
import scala.scalajs.js.timers.SetTimeoutHandle

import demo.Location
import demo.Weather
import demo.WorldCity
import japgolly.scalajs.react.ReactEventAliases
import japgolly.scalajs.react.SyntheticEvent
import japgolly.scalajs.react.vdom.all.dangerouslySetInnerHtml
import autowire._
import boopickle.Default._
import demo.SampleApi
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB}
import org.scalajs.dom.html
import org.w3c.dom.html.HTMLInputElement
import service.SampleClient
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
  * Main Screen component
  */
object MyScreenPage extends ReactEventAliases {

  case class State(location: Option[Location],
                   locationOptions: Seq[WorldCity],
                   weather: Option[Weather])
  object State {
    val default = State(None, Seq.empty[WorldCity], None)
  }

  class Backend($ : BackendScope[Unit, State]) {
    def init() = {
      Callback {
        getWeather(None).map(
            response => $.modState(_.copy(weather = response)).runNow())
      }
    }

    def getWeather(location: Option[Location]): Future[Option[Weather]] =
      SampleClient[SampleApi].weatherplz($.state.runNow().location).call()

    var timeout: SetTimeoutHandle = _

    import scala.scalajs.js.timers._

    def setLocation(location: WorldCity)(e: ReactEventI): Callback = Callback {
      println(s"set location: $location")
      clearTimeout(timeout)
      timeout = setTimeout(1000.0) {
        for {
          weather <- getWeather(Some(location.toLocation)) if weather.isDefined
        } {
          println(s"new weather: $weather")
          $.modState(_.copy(location = Some(location.toLocation),
                             weather = weather))
            .runNow()
        }
      }
    }

    def onInputChanged(e: ReactEventI): Callback =
      Callback {
        SampleClient[SampleApi].cities(e.target.value).call().map { response =>
          println(response)
          $.modState(_.copy(locationOptions = response)).runNow()
        }
      }

    def locationForm(locations: Seq[WorldCity]) =
      <.div(
          <.h1("Change your location:"),
          <.input(^.onChange ==> onInputChanged),
          <.ul(
              locations.map { location =>
                <.button(^.onClick ==> setLocation(location), location.display)
              }: _*
          ))

    def render(): ReactTagOf[html.Div] = {
//      println($.state.runNow().weather)
      val s = $.state.runNow()
      val weatherDiv = s.weather.fold({
        <.div(<.h1("Where are you?"))
      }) { weather =>
        val weatherMsg = weather.kind match {
          case "PartlyCloud" => "Almost there, it's partly cloudy"
          case "Sun" => "Yay, it's sunny"
          case "Rain" => "Bring your umbrella, it's raining"
          case "Cloud" => "Sorry, it's cloudy"
          case "LightCloud" => "Not so bad, light clouds"
          case x => s"Awww, it's $x"
        }
        val location = s.location.getOrElse(weather.location)
        val userMsg = s"$weatherMsg in ${location.city}, ${location.country}."
        <.div(<.h1(userMsg), <.div(dangerouslySetInnerHtml(weather.iconSvg)))
      }
      <.div(weatherDiv, locationForm($.state.runNow().locationOptions))
    }
  }

  val component = ReactComponentB[Unit]("MyScreenPage")
    .initialState(State.default)
    .renderBackend[Backend]
    .componentDidMount(_.backend.init())
    .buildU
}
