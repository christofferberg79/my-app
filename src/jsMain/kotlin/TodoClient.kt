package cberg.myapp

import cberg.myapp.model.Todo
import cberg.myapp.model.TodoDraft
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.contentType
import kotlin.browser.window

class TodoClient {
    private val baseUrl = Url(window.location.origin)
    private val todosUrl = URLBuilder(baseUrl).path("todos").build()
    private val String.url get() = URLBuilder(baseUrl).path("todos", this).build()
    private val client: HttpClient = HttpClient(Js) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }

    fun close() = client.close()

    suspend fun get() = client.get<List<Todo>>(todosUrl)

    suspend fun create(description: String) = client.post<Unit>(todosUrl) {
        contentType(ContentType.Application.Json)
        body = TodoDraft(description)
    }

    suspend fun update(todo: Todo) = client.put<Unit>(todo.id.url) {
        contentType(ContentType.Application.Json)
        body = TodoDraft(todo.description, todo.done)
    }

    suspend fun delete(id: String) = client.delete<Unit>(id.url)
}