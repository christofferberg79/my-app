package cberg.myapp

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.DriverManager
import java.util.*

object Todos : Table("todo") {
    val id = uuid("id")
    val description = varchar("description", 255)
}

data class Todo(val id: UUID, val description: String)
data class TodoDraft(val description: String)

fun Application.main() {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    Database.connect(getNewConnection = {
        val url = System.getenv("JDBC_DATABASE_URL")
        DriverManager.getConnection(url)
    })

    routing {
        get("/todos") {
            val todos = transaction {
                Todos
                    .selectAll()
                    .map { Todo(it[Todos.id], it[Todos.description]) }
            }

            call.respond(todos)
        }

        post("/todos") {
            val todoDraft = call.receive<TodoDraft>()
            val todo = Todo(UUID.randomUUID(), todoDraft.description)
            transaction {
                Todos.insert {
                    it[id] = todo.id
                    it[description] = todo.description
                }
            }
            call.response.status(HttpStatusCode.Created)
            call.response.header(HttpHeaders.Location, "/todos/${todo.id}")
        }

        get("/todos/{id}") {
            val id = call.parameters["id"]?.let { UUID.fromString(it) }
            check(id != null)
            val todo = transaction {
                Todos.select { Todos.id eq id }
                    .map { Todo(it[Todos.id], it[Todos.description]) }
                    .single()
            }
            call.respond(todo)
        }
    }
}
