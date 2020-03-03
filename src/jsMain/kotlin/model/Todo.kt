package cberg.myapp.model

import kotlinx.serialization.*

@Serializable
data class Todo(val id: String, val description: String, val done: Boolean)

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

@Serializable
data class TodoDraft(val description: String, val done: Boolean = false)