package style

import csstype.Length
import react.ChildrenBuilder

fun ChildrenBuilder.centeredPaneFlex(key: String, maxWidth: Length, block: ChildrenBuilder.()->Unit) {
    styledDiv(key, flexChild(), flexContainerHorizontal()) {
        styledDiv("L", flexChild()) {}
        styledDiv("C", flexChild(grow = 100.0), { this.maxWidth = maxWidth }, flexContainerVertical()) {
            block()
        }
        styledDiv("R", flexChild()) {}
    }
}