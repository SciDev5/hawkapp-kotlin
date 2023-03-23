package style

import csstype.*
import react.ChildrenBuilder


fun ChildrenBuilder.flexDivider(n: Int, direction: FlexDirection, margin: Length? = null) = styledDiv(
    "d-$n",
    flexChild(0.0, 0.0),
    {
        val border = Border(1.0.px, LineStyle.solid, StyleColors.border)
        if (direction == FlexDirection.column || direction == FlexDirection.columnReverse)
            borderTop = border
        else
            borderLeft = border
        this.margin = margin
    }
) {}

fun ChildrenBuilder.flexDividerVertical(n: Int, margin: Length? = null) = flexDivider(n, FlexDirection.row, margin)
fun ChildrenBuilder.flexDividerHorizontal(n: Int, margin: Length? = null) = flexDivider(n, FlexDirection.column, margin)
fun flexContainerHorizontal() = flexContainer(FlexDirection.row)
fun flexContainerVertical() = flexContainer(FlexDirection.column)
fun flexContainer(direction: FlexDirection): CSSCallback = {
    display = Display.flex
    flexDirection = direction
    position = Position.relative
}

fun flexChild(grow: Double = 1.0, shrink: Double = 1.0): CSSCallback = {
    flexGrow = number(grow)
    flexShrink = number(shrink)
}