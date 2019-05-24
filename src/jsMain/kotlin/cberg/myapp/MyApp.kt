import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.serialization.*
import kotlin.browser.document
import kotlin.browser.window

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

fun main() {
    window.onload = {
        val root = document.getElementById("root") ?: throw IllegalStateException("No root element found")
        root.append {
            h1 { +"Things to do" }
            table {
                id = "todo-table"
                tr {
                    th { +"Description" }
                }
            }
        }
        GlobalScope.launch {
            val client = HttpClient(Js) {
                install(JsonFeature) {
                    serializer = KotlinxSerializer()
                }
            }

            val todos = client.get<TodoList>("${window.location}todos").items

            document.getElementById("todo-table")?.append {
                todos.forEach {
                    tr {
                        td { +it.description }
                    }
                }
            }
            client.close()
        }
    }
}