package cberg.myapp

import cberg.myapp.model.Todo
import kotlinx.css.*
import kotlinx.css.properties.lh
import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.dom.button
import react.dom.td
import react.dom.tr
import styled.css
import styled.styledDiv

fun RBuilder.todoItem(todo: Todo, onDone: (Boolean) -> Unit, onDelete: () -> Unit) {

    tr {
        key = todo.id
        td { +todo.description }
        td {
            checkbox(todo.done, onDone)
        }
        td {
            button("Delete", onDelete)
        }
    }
}

fun RBuilder.button(text: String, onClick: () -> Unit) {
    button {
        +text
        attrs.onClickFunction = { onClick() }
    }
}

fun RBuilder.checkbox(value: Boolean, onChange: (Boolean) -> Unit) {
    styledDiv {
        +if (value) "\u2611" else "\u2610"
        attrs.onClickFunction = { onChange(!value) }
        css {
            display = Display.inlineBlock
            lineHeight = 0.px.lh
            cursor = Cursor.default
        }
    }
}
