package cberg.myapp

import cberg.myapp.model.Todo
import react.RBuilder
import react.dom.*

fun RBuilder.todoTable(todos: List<Todo>, delete: (String) -> Unit, update: (Todo) -> Unit) {
    table {
        thead {
            tr {
                th { +"Description" }
                th { +"Done" }
            }
        }
        tbody {
            todos.forEach { todo ->
                todoItem(todo, update, delete)
//                tr {
//                    key = todo.id
//                    td {
//                        +todo.description
//                    }
//                    td {
//                        checkbox(todo.done) { done ->
//                            update(todo.copy(done = done))
//                        }
//                    }
//                    td {
//                        button {
//                            +"Delete"
//                            attrs.onClickFunction = { delete(todo.id) }
//                        }
//                    }
//                }
            }
        }
    }
}

