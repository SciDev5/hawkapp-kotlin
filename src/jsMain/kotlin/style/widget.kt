package style

import csstype.*

object InputStyle {

    val base: CSSCallback = {
        fontFamily = getSansFont()
        background = StyleColors.bgLighten
        color = StyleColors.fgMain
        border = Border(0.0.px, LineStyle.hidden)
        paddingInline = 0.5.em
        paddingBlock = 0.3.em

        transition = StyleColors.bgFgTransition

        not(":disabled") {
            hover {
                background = StyleColors.bgLightenHov
                color = StyleColors.fgMain
            }
        }
        disabled {
            opacity = number(0.5)
        }
    }

    val lined : CSSCallback = {
        base()
        background = StyleColors.transparent
        color = StyleColors.fgMain
        border = Border(1.0.px, LineStyle.solid, StyleColors.border)
    }
    val muted : CSSCallback = {
        base()
        background = StyleColors.transparent
        color = StyleColors.fgMain
    }
    val emphatic : CSSCallback = {
        base()
        background = StyleColors.bgEm
        color = StyleColors.fgEm

        not(":disabled") {
            hover {
                background = StyleColors.bgEmHov
                color = StyleColors.fgEm
            }
        }
    }
}
