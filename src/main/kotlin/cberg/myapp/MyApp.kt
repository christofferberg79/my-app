package cberg.myapp

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpHeaders.Location
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.http.HttpStatusCode.Companion.NotFound
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

private fun Todo(it: ResultRow) = Todo(it[Todos.id], it[Todos.description])

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
                    Todos.selectAll().map { Todo(it) }
                }
                call.respond(todos)
            }

            post {
                val todoDraft = try {
                    call.receive<TodoDraft>()
                } catch (e: Exception) {
                    call.response.status(BadRequest)
                    return@post
                }
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
                    val id = try {
                        UUID.fromString(call.parameters["id"])
                    } catch (e: IllegalArgumentException) {
                        call.response.status(NotFound)
                        return@get
                    }
                    val todo = transaction {
                        Todos.select { Todos.id eq id }.singleOrNull()?.let { Todo(it) }
                    }
                    if (todo == null) {
                        call.response.status(NotFound)
                    } else {
                        call.respond(todo)
                    }
                }

                put {
                    val id = try {
                        UUID.fromString(call.parameters["id"])
                    } catch (e: IllegalArgumentException) {
                        call.response.status(NotFound)
                        return@put
                    }
                    val todoDraft = try {
                        call.receive<TodoDraft>()
                    } catch (e: Exception) {
                        call.response.status(BadRequest)
                        return@put
                    }
                    val count = transaction {
                        Todos.update({ Todos.id eq id }) {
                            it[description] = todoDraft.description
                        }
                    }
                    if (count == 0) {
                        call.response.status(NotFound)
                    } else {
                        call.response.status(NoContent)
                    }
                }

                delete {
                    val id = try {
                        UUID.fromString(call.parameters["id"])
                    } catch (e: IllegalArgumentException) {
                        call.response.status(NotFound)
                        return@delete
                    }
                    val count = transaction {
                        Todos.deleteWhere { Todos.id eq id }
                    }
                    if (count == 0) {
                        call.response.status(NotFound)
                    } else {
                        call.response.status(NoContent)
                    }
                }
            }
        }
    }
}
