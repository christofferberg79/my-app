package cberg.myapp

import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.RBuilder
import react.dom.input

fun RBuilder.todoDone(done: Boolean, update: (Boolean) -> Unit) {
    input(InputType.checkBox) {
        attrs {
            defaultChecked = done
            onChangeFunction = { event ->
                val target = event.target as HTMLInputElement
                update(target.checked)
            }
        }
    }
}
