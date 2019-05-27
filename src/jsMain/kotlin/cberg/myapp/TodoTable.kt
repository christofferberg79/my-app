package cberg.myapp

import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.*

class TodoTable : RComponent<TodoTable.Props, RState>() {
    override fun RBuilder.render() {
        table {
            thead {
                tr {
                    th { +"Description" }
                }
            }
            tbody {
                props.todos.forEach {
                    tr {
                        td { +it.description }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var todos: List<Todo>
    }
}

fun RBuilder.todoTable(todos: List<Todo>) = child(TodoTable::class) {
    attrs.todos = todos
}