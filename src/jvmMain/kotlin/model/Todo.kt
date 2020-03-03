package cberg.myapp.model

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import java.util.*

interface Todo {
    val description: String
    val done: Boolean
}

data class TodoWithId(val id: UUID, override val description: String, override val done: Boolean) : Todo
data class TodoDraft(override val description: String, override val done: Boolean) : Todo

fun TodoDraft.withId() = TodoWithId(UUID.randomUUID(), description, done)

object Todos : Table("todo") {
    val id = uuid("id")
    val description = varchar("description", 255)
    val done = bool("done")
}

fun Todos.set(statement: UpdateBuilder<Number>, todo: TodoWithId) {
    statement[id] = todo.id
    set(statement, todo as Todo)
}

fun Todos.set(statement: UpdateBuilder<Number>, todo: Todo) {
    statement[description] = todo.description
    statement[done] = todo.done
}

fun Todo(row: ResultRow): Todo = TodoWithId(
    id = row[Todos.id],
    description = row[Todos.description],
    done = row[Todos.done]
)
