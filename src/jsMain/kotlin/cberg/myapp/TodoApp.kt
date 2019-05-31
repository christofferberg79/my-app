package cberg.myapp

import cberg.myapp.model.Todo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import react.*
import react.dom.h1
import kotlin.browser.window

class TodoApp : RComponent<RProps, TodoApp.State>(), CoroutineScope by MainScope() {
    private lateinit var client: TodoClient
    private var timeoutId: Int? = null

    override fun State.init() {
        todos = emptyList()
    }

    override fun componentDidMount() {
        client = TodoClient()
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
            client.create(description)
            reloadTodos()
        }
    }

    private fun onUpdate(todo: Todo) {
        launch {
            client.update(todo)
            reloadTodos()
        }
    }

    private fun onDelete(todo: Todo) {
        launch {
            client.delete(todo.id)
            reloadTodos()
        }
    }

    private suspend fun reloadTodos() {
        val newTodos = client.get().sortedBy { it.description }
        setState {
            todos = newTodos
        }
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
