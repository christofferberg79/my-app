package cberg.myapp

import kotlinx.html.js.onClickFunction
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
                props.todos.forEach { todo ->
                    tr {
                        td { +todo.description }
                        td {
                            button {
                                +"Delete"
                                attrs.onClickFunction = { props.delete(todo.id) }
                            }
                        }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var todos: List<Todo>
        var delete: (String) -> Unit
    }
}

fun RBuilder.todoTable(todos: List<Todo>, delete: (String) -> Unit) = child(TodoTable::class) {
    attrs.todos = todos
    attrs.delete = delete
}