package cberg.myapp

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.html.js.onClickFunction
import kotlinx.io.core.use
import react.*
import react.dom.button
import react.dom.h1
import kotlin.browser.window

class TodoApp : RComponent<RProps, TodoApp.State>(), CoroutineScope by MainScope() {
    init {
        state.apply {
            todos = emptyList()
        }
    }

    override fun componentDidMount() {
        fetchTodos()
    }

    private fun CoroutineScope.fetchTodos() = launch {
        val client = HttpClient(Js) {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
        }
        client.use {
            val newTodos = it.get<TodoList>("${window.location}todos").items
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
        button {
            +"Refresh"
            attrs.onClickFunction = { fetchTodos() }
        }
        todoTable(state.todos)
    }

    interface State : RState {
        var todos: List<Todo>
    }
}

fun RBuilder.app() = child(TodoApp::class) {}
