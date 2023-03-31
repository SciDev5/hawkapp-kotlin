package util.react

import csstype.*
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import react.*
import react.dom.html.ReactHTML
import style.*
import util.TBox
import util.coroutine.KPromise
import kotlin.random.Random

class Modals(
    private val modalList: MutableList<Modal>,
) {
    lateinit var update: () -> Unit
    fun displayModal(modal: Modal) {
        modal.hideCallback = {
            modalList.remove(modal)
            update()
        }
        modalList.add(modal)
        update()
    }
}

fun useQuestionModal(question: String) = UseModal {
    ResolutionModal<String?>(
        defaultRes = { null }
    ) { res, escape ->
        var contentState by useState("")
        styled(ReactHTML.h3, "h", flexChild(0.0)) {
            +question
        }
        styledDiv("response", flexChild(0.0), flexContainerHorizontal()) {
            styled(ReactHTML.input, "i", InputStyle.lined, flexChild()) {
                value = contentState
                onChange = { contentState = it.currentTarget.value }
                onKeyDown = {
                    if (it.key == "Enter") res(contentState)
                }

                val ref = useRef<HTMLInputElement>()
                this.ref = ref
                useEffect {
                    ref.current?.focus()
                }
            }
            styled(ReactHTML.button, "b", InputStyle.emphatic, flexChild(0.0)) {
                +"ok"
                onKeyDown = {
                    when (it.key) {
                        "Enter", " " -> res(contentState)
                    }
                }
                onClick = {
                    res(contentState)
                }
            }
            styled(ReactHTML.button, "c", InputStyle.lined, flexChild(0.0)) {
                +"cancel"
                onKeyDown = {
                    when (it.key) {
                        "Enter", " " -> escape()
                    }
                }
                onClick = {
                    escape()
                }
            }
        }
    }
}

fun useModals() = useContext(modalsContext) ?: throw Error("could not find modals context in scope")
class UseModal<T : Modal>(private val genModal: () -> T) {
    private val modals = useModals()

    fun show() = genModal().also { modals.displayModal(it) }
}

suspend fun <T> UseModal<ResolutionModal<T>>.request() = show().await()

typealias HideCallback = () -> Unit

class BasicModal(
    override val maxWidth: Length = 30.0.em,
    private val block: ChildrenBuilder.(hide: HideCallback) -> Unit
) : Modal {
    override lateinit var hideCallback: HideCallback
    override fun display(c: ChildrenBuilder) = block(c, hideCallback)
    override fun escape() {
        hideCallback()
    }
}

class ResolutionModal<T>(
    override val maxWidth: Length = 30.0.em,
    private val defaultRes: (() -> T)? = null,
    private val block: ChildrenBuilder.(resolve: (T) -> Unit, escape: () -> Unit) -> Unit,
) : Modal {
    override lateinit var hideCallback: HideCallback
    private val promise = KPromise<TBox<T>>()
    suspend fun await() = promise.await().contents
    override fun display(c: ChildrenBuilder) = block(
        c,
        { resolution ->
            promise.resolve(TBox(resolution))
            hideCallback()
        },
        { escape() }
    )

    override fun escape() {
        defaultRes?.let {
            promise.resolve(TBox(it()))
            hideCallback()
        }
        // otherwise
        // there is no escape
    }
}

interface Modal {
    val maxWidth: Length
    var hideCallback: HideCallback
    fun display(c: ChildrenBuilder)
    fun escape()
}

private val modalsContext = createContext<Modals?>(null)

private external interface MPProps : Props {
    var modalDisplayList: List<Modal>
    var modals: Modals
}

private val ModalsShower = FC<MPProps> { props ->
    var update by useState(0)
    props.modals.update = { update = Random.nextInt() }

    for (modal in props.modalDisplayList) {
        styled(ReactHTML.div, "modals", flexContainerVertical(), {
            position = Position.absolute
            left = 0.0.px
            right = 0.0.px
            top = 0.0.px
            bottom = 0.0.px
            paddingTop = 10.0.vh
            zIndex = "9999999".unsafeCast<ZIndex>()
            background = Color("#7774")
        }) {
            onClick = { e ->
                console.log(e.target, e.currentTarget)
                val target = e.target
                if (target is HTMLElement && target.parentElement?.parentElement == e.currentTarget) {
                    modal.escape()
                }
            }
            useEffect {
                { e: KeyboardEvent ->
                    if (e.key == "Escape")
                        modal.escape()
                }.unsafeCast<(Event) -> Unit>().also {
                    window.addEventListener("keydown", it)
                    cleanup {
                        window.removeEventListener("keydown", it)
                    }
                }
            }
            centeredPaneFlex("", modal.maxWidth) {
                styledDiv("", {
                    background = StyleColors.bgMain
                    border = Border(1.0.px, LineStyle.solid, StyleColors.fgMain)
                    padding = 2.0.em
                }) {
                    modal.display(this)
                }
            }
        }
    }
}

fun ChildrenBuilder.provideModals(block: ChildrenBuilder.() -> Unit) {
    val modalDisplayList = useMemo { mutableListOf<Modal>() }
    val contextMemo = useMemo(modalDisplayList) {
        Modals(modalDisplayList)
    }
    ModalsShower {
        this.modalDisplayList = modalDisplayList
        this.modals = contextMemo
    }

    modalsContext.Provider {
        this.value = contextMemo
        block()
    }
}