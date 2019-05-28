package cberg.myapp

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import react.*
import react.dom.h1
import kotlin.browser.window

class TodoApp : RComponent<RProps, TodoApp.State>(), CoroutineScope by MainScope() {
    lateinit var client: HttpClient

    init {
        state.todos = emptyList()
    }

    override fun componentDidMount() {
        client = HttpClient(Js) {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
        }
        launch { fetchTodos() }
    }

    private suspend fun fetchTodos() {
        val newTodos = client.get<TodoList>("${window.location}todos").items
        setState {
            todos = newTodos
        }
    }

    private fun onDelete(id: String) {
        launch {
            deleteTodo(id)
            fetchTodos()
        }
    }

    private fun onAdd(description: String) {
        launch {
            createTodo(description)
            fetchTodos()
        }
    }

    private suspend fun deleteTodo(id: String) {
        client.delete<Unit>("${window.location}todos/$id")
    }

    private suspend fun createTodo(description: String) {
        client.post<Unit>("${window.location}todos") {
            contentType(ContentType.Application.Json)
            body = TodoDraft(description)
        }
        println("after post")
    }

    override fun componentWillUnmount() {
        cancel()
        client.close()
    }

    override fun RBuilder.render() {
        h1 { +"Things to do" }
        todoAdder(::onAdd)
        todoTable(state.todos, ::onDelete)
    }

    interface State : RState {
        var todos: List<Todo>
    }
}

fun RBuilder.app() = child(TodoApp::class) {}
