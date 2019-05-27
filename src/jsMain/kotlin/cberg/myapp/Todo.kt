package cberg.myapp

import kotlinx.serialization.*

@Serializable
data class Todo(val id: String, val description: String)

@Serializable
class TodoList(val items: List<Todo>) {
    @Serializer(TodoList::class)
    companion object : KSerializer<TodoList> {
        override fun serialize(encoder: Encoder, obj: TodoList) {
            Todo.serializer().list.serialize(encoder, obj.items)
        }

        override fun deserialize(decoder: Decoder): TodoList {
            return TodoList(Todo.serializer().list.deserialize(decoder))
        }
    }
}
