package cberg.myapp

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpHeaders.Location
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.request.uri
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.*
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
        route("todos") {
            get {
                val todos = transaction {
                    Todos.selectAll()
                        .map { Todo(it[Todos.id], it[Todos.description]) }
                }

                call.respond(todos)
            }

            post {
                val todoDraft = call.receive<TodoDraft>()
                val todo = Todo(UUID.randomUUID(), todoDraft.description)
                transaction {
                    Todos.insert {
                        it[id] = todo.id
                        it[description] = todo.description
                    }
                }
                call.response.status(Created)
                call.response.header(Location, "${call.request.uri}/${todo.id}")
            }

            route("{id}") {
                get {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    check(id != null)
                    val todo = transaction {
                        Todos.select { Todos.id eq id }
                            .map { Todo(it[Todos.id], it[Todos.description]) }
                            .single()
                    }
                    call.respond(todo)
                }

                delete {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    check(id != null)
                    transaction {
                        Todos.deleteWhere { Todos.id eq id }
                    }
                    call.response.status(NoContent)
                }
            }
        }
    }
}
