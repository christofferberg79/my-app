package cberg.myapp

import kotlinx.html.js.onClickFunction
import kotlinx.html.style
import react.RBuilder
import react.dom.div

fun RBuilder.checkbox(choice: Boolean, update: (Boolean) -> Unit) {
    div(classes = "checkbox") {
        +if (choice) "\u2611" else "\u2610"
        attrs.onClickFunction = { update(!choice) }
        attrs.style = kotlinext.js.js {
            display = "inline-block"
            lineHeight = "0"
            cursor = "default"
        }
    }
}