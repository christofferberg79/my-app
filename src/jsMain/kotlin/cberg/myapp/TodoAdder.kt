package cberg.myapp

import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import react.*
import react.dom.button
import react.dom.input

class TodoAdder : RComponent<TodoAdder.Props, TodoAdder.State>() {
    init {
        state.description = ""
    }

    override fun RBuilder.render() {
        input {
            attrs {
                value = state.description
                placeholder = "What to do?"
                onChangeFunction = ::onInputChange
            }
        }
        button {
            +"Add"
            attrs.onClickFunction = { props.add(state.description) }
        }
    }

    private fun onInputChange(event: Event) {
        val target = event.target as HTMLInputElement
        setState {
            description = target.value
        }
    }

    interface Props : RProps {
        var add: (String) -> Unit
    }

    interface State : RState {
        var description: String
    }
}

fun RBuilder.todoAdder(add: (String) -> Unit) = child(TodoAdder::class) {
    attrs.add = add
}