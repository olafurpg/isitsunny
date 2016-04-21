package pages

import demo.Weather
import japgolly.scalajs.react.vdom.all.dangerouslySetInnerHtml
import autowire._
import boopickle.Default._
import demo.SampleApi
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB}
import org.scalajs.dom.html
import service.SampleClient
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
  * Main Screen component
  */
object MyScreenPage {

  case class State(weather: Option[Weather])

  class Backend($ : BackendScope[Unit, State]) {
    def init() = {
      Callback {
        SampleClient[SampleApi]
          .weatherplz()
          .call()
          .map(response =>
                {
              $.modState(_.copy(weather = response)).runNow()
          })
      }
    }
    def render(): ReactTagOf[html.Div] = {
      println($.state.runNow().weather)
      $.state
        .runNow()
        .weather
        .fold({
          <.div(<.h1("Where are you?"))
        }) { weather =>
          val weatherMsg = weather.kind match {
            case "PartyCloudy" => "Almost there, it's partly cloudy"
            case "Sunny" => "Yay, it's sunny"
            case "Cloud" => "Sorry, it's raining"
            case x => s"Awww, it's $x"
          }
          val userMsg =
            s"$weatherMsg in ${weather.location.city}, ${weather.location.country}."
          <.div(<.h1(userMsg), <.div(dangerouslySetInnerHtml(weather.iconSvg)))
        }
    }
  }

  val component = ReactComponentB[Unit]("MyScreenPage")
    .initialState(State(None))
    .renderBackend[Backend]
    .componentDidMount(_.backend.init())
    .buildU
}
