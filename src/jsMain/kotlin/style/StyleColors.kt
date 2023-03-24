package style

import csstype.Color
import csstype.Time
import csstype.Transition
import csstype.s

object StyleColors {
    val transparent = Color("#0000")

    val border = Color("#7775")

    val bgMain = Color("#111012")
    val bgLighten = Color("#fff1")
    val bgLightenHov = Color("#fff4")
    val fgMain = Color("#fff")
    val bgEm = Color("#74f")
    val bgEmHov = Color("#2ecae3")
    val fgEm = Color("#fff")

    val colorful = arrayOf("#2effa1","#972eff", "#ff902e").map {
        Pair(Color(it), Color(it + "40"))
    }

    val cssBGMain: CSSCallback = { background = bgMain }
    val cssFGMain: CSSCallback = { color = fgMain }

    val warnBg = Color("#db1e535e")

    val bgFgTransition = cssTransition(0.3.s,"color", "background-color")

    fun cssTransition(time: Time, vararg properties: String) =
        properties.map {
            "$time $it"
        }.joinToString(", ").unsafeCast<Transition>()
}