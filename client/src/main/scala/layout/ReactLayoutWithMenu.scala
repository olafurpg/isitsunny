package layout

import component.{Menu, Route}
import core.material._
import japgolly.scalajs.react.extra.OnUnmount
import japgolly.scalajs.react.extra.router.{Path, RouterCtl}
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, PropsChildren, ReactComponentB, ReactNode}
import router.ApplicationRouter.Loc

case class HeaderConfig(router: RouterCtl[Loc] = null,
                        title: String = "Sample Title",
                        menu: List[Menu] = List(),
                        topLinks: Option[TagMod] = None)

object ReactLayoutWithMenu {

  class Backend($ : BackendScope[HeaderConfig, Unit]) extends OnUnmount {
    def render(P: HeaderConfig, C: PropsChildren) = {
      <.div(^.cls := "mdl-layout__container")(
            <.div(
                ^.cls := "mdl-layout mdl-js-layout mdl-layout--fixed-drawer")(
                <.main(^.cls := "mdl-layout__content")(C)
            )
        )
    }
  }

  val component = ReactComponentB[HeaderConfig]("Application-Header")
    .renderBackend[Backend]
    .build

  def apply(config: HeaderConfig)(nodes: ReactNode*) =
    component(config, nodes: _*)
}
