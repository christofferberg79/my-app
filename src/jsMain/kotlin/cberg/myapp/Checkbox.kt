package cberg.myapp

import kotlinx.css.Cursor
import kotlinx.css.Display
import kotlinx.css.properties.lh
import kotlinx.css.px
import kotlinx.html.js.onClickFunction
import react.RBuilder
import styled.css
import styled.styledDiv

fun RBuilder.checkbox(choice: Boolean, update: (Boolean) -> Unit) {
    styledDiv {
        +if (choice) "\u2611" else "\u2610"
        attrs.onClickFunction = { update(!choice) }
        css {
            display = Display.inlineBlock
            lineHeight = 0.px.lh
            cursor = Cursor.default
        }
    }
}