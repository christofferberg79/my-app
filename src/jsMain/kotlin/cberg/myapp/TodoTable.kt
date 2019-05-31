package cberg.myapp

import cberg.myapp.model.Todo
import react.RBuilder
import react.dom.*

fun RBuilder.todoTable(todos: List<Todo>, delete: (Todo) -> Unit, update: (Todo) -> Unit) {
    table {
        thead {
            tr {
                th { +"Description" }
                th { +"Done" }
            }
        }
        tbody {
            todos.forEach { todo ->
                todoItem(todo,
                    onDone = { done -> update(todo.copy(done = done)) },
                    onDelete = { delete(todo) })
            }
        }
    }
}

