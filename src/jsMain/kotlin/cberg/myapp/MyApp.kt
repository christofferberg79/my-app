import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import kotlinx.coroutines.*
import kotlinx.serialization.*
import react.*
import react.dom.*
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
        render(root) {
            app()
        }
    }
}

class App : RComponent<RProps, App.State>(), CoroutineScope by MainScope() {
    init {
        state.apply {
            todos = emptyList()
        }
    }

    override fun componentDidMount() {
        launch {
            val client = HttpClient(Js) {
                install(JsonFeature) {
                    serializer = KotlinxSerializer()
                }
            }
            val newTodos = client.get<TodoList>("${window.location}todos").items
            client.close()

            setState {
                todos = newTodos
            }
        }
    }

    override fun componentWillUnmount() {
        cancel()
    }

    override fun RBuilder.render() {
        h1 { +"Things to do" }
        table {
            thead {
                tr {
                    th { +"Description" }
                }
            }
            tbody {
                state.todos.forEach {
                    tr {
                        td { +it.description }
                    }
                }
            }
        }
    }

    interface State : RState {
        var todos: List<Todo>
    }
}

private fun RBuilder.app() = child(App::class) {}
