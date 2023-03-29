import csstype.*
import react.FC
import react.Props
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h3
import react.router.dom.Link
import style.*


val AppLoggedOut = FC<Props> { _ ->
    styledDiv("bg",{
        zIndex = "-1".unsafeCast<ZIndex>()
        position = Position.absolute
        top = 0.0.px
        bottom = 0.0.px
        left = 0.0.px
        right = 0.0.px
        mixBlendMode = "lighten".unsafeCast<MixBlendMode>()
    }) {
        styledDiv("bgl",{
            position = Position.absolute
            top = 0.0.px
            bottom = 0.0.px
            left = 0.0.px
            right = 0.0.px
            background = "linear-gradient(-30deg, #7fa, #0000), linear-gradient(-0deg, #73ff, #0000), linear-gradient(90deg, #f3ff, #0000), #d7f".unsafeCast<Background>()
        }) {}
        styledDiv("bgk",{
            position = Position.absolute
            top = 0.0.px
            bottom = 0.0.px
            left = 0.0.px
            right = 0.0.px
            mixBlendMode = "multiply".unsafeCast<MixBlendMode>()
            background = "linear-gradient(60deg, #0000 10%, #fff7 10%, #fff7 15%, #0000 15%, #0009 18%, #eef6 18%, #fee7 20%, #0000 20%), linear-gradient(100deg, #0000 2%, #fdf5 18%, #dff6 20%, #0000 20%), #000".unsafeCast<Background>()
        }) {}
    }
    styledDiv("main", {
        background = "linear-gradient(-30deg, #7fa, #0000), linear-gradient(-0deg, #73ff, #0000), linear-gradient(90deg, #f3ff, #0000), #d7f".unsafeCast<Background>()
        backgroundClip = "text".unsafeCast<BackgroundClip>()
        color = StyleColors.transparent
        marginInline = 10.0.vw
        marginTop = 10.0.vh
    }) {
        styled(h1, "title",{
            marginBlock = 0.1.em
            fontSize = 30.0.vh
            fontFamily = getTitleFont()

            background = "linear-gradient(#0000 30%, #0001)".unsafeCast<Background>()
        }) {
            +"HawkApp"
        }
        styled(h3, "subtitle",{
            color = Color("#0004")
        }) {
            +"Digital Announcements and Messaging"
        }
        styled(Link, "login", InputStyle.emphatic, {
            display = Display.block
            fontSize = 10.0.vh
            fontFamily = getTitleFont()

            float = Float.right
            marginTop = 8.0.vh
            paddingInline = 20.0.vw
        }) {
            to = Endpoints.Page.login
            +":: login ::"
        }
    }
}