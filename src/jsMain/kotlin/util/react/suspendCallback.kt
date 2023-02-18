package util.react

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun <T> suspendCallback(
    coroutineScope: CoroutineScope,
    block: suspend (T)->Unit
) = { e:T ->
    coroutineScope.launch {
        block(e)
    }
    Unit
}