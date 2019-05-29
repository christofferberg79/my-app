package cberg.myapp

import cberg.myapp.model.Todo
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
                    th { +"Done" }
                }
            }
            tbody {
                props.todos.forEach { todo ->
                    tr {
                        key = todo.id
                        td {
                            +todo.description
                        }
                        td {
                            checkbox(todo.done) { done ->
                                props.update(todo.copy(done = done))
                            }
                        }
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
        var update: (Todo) -> Unit
    }
}

fun RBuilder.todoTable(todos: List<Todo>, delete: (String) -> Unit, update: (Todo) -> Unit) =
    child(TodoTable::class) {
        attrs.todos = todos
        attrs.delete = delete
        attrs.update = update
    }

