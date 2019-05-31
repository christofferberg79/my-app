package cberg.myapp

import cberg.myapp.model.Todo
import cberg.myapp.model.TodoDraft
import cberg.myapp.model.TodoList
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
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
    private lateinit var client: HttpClient
    private var timeoutId: Int? = null

    override fun State.init() {
        todos = emptyList()
    }

    override fun componentDidMount() {
        client = HttpClient(Js) {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
        }
        launch { reloadTodos() }
        timeoutId = window.setInterval({ launch { reloadTodos() } }, 5000)
    }

    override fun componentWillUnmount() {
        timeoutId?.let {
            window.clearInterval(it)
            timeoutId = null
        }
        cancel()
        client.close()
    }

    private fun onAdd(description: String) {
        launch {
            createTodo(description)
            reloadTodos()
        }
    }

    private fun onUpdate(todo: Todo) {
        launch {
            updateTodo(todo)
            reloadTodos()
        }
    }

    private fun onDelete(todo: Todo) {
        launch {
            deleteTodo(todo.id)
            reloadTodos()
        }
    }

    private suspend fun reloadTodos() {
        val newTodos = getTodos().sortedBy { it.description }
        setState {
            todos = newTodos
        }
    }

    private suspend fun getTodos() = client.get<TodoList>("${window.location.origin}/todos").items

    private suspend fun createTodo(description: String) {
        client.post<Unit>("${window.location.origin}/todos") {
            contentType(ContentType.Application.Json)
            body = TodoDraft(description)
        }
    }

    private suspend fun updateTodo(todo: Todo) {
        client.put<Unit>("${window.location.origin}/todos/${todo.id}") {
            contentType(ContentType.Application.Json)
            body = TodoDraft(todo.description, todo.done)
        }
    }

    private suspend fun deleteTodo(id: String) {
        client.delete<Unit>("${window.location.origin}/todos/$id")
    }

    override fun RBuilder.render() {
        h1 { +"Things to do" }
        todoAdder(::onAdd)
        todoTable(state.todos, ::onDelete, ::onUpdate)
    }

    interface State : RState {
        var todos: List<Todo>
    }
}

fun RBuilder.app() = child(TodoApp::class) {}
