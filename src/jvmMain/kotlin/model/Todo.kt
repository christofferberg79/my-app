package cberg.myapp.model

import kotlinx.serialization.*
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import java.util.*

interface Todo {
    val description: String
    val done: Boolean
}

@Serializable
data class TodoWithId(
    @Serializable(UUIDSeralizer::class) val id: UUID,
    override val description: String,
    override val done: Boolean
) : Todo

@Serializable
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

@Serializer(UUID::class)
object UUIDSeralizer : KSerializer<UUID> {
    override val descriptor = PrimitiveDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}