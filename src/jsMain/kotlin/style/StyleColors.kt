package style

import csstype.Color

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

    val cssBGMain: CSSCallback = { background = bgMain }
    val cssFGMain: CSSCallback = { color = fgMain }

    val warnBg = Color("#db1e535e")
}