package cberg.myapp

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.BadRequestException
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.features.NotFoundException
import io.ktor.http.HttpHeaders.Location
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.request.uri
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.util.KtorExperimentalAPI
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

@KtorExperimentalAPI
fun Application.main() {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    install(CORS) {
        anyHost()
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
                val todoDraft = call.receiveOrBadRequestException<TodoDraft>()
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
                    val id = uuidOrNotFoundException(call.parameters["id"])
                    val todo = transaction {
                        Todos.select { Todos.id eq id }.singleOrNull()
                            ?.let { Todo(it) }
                            ?: throw NotFoundException()
                    }
                    call.respond(todo)
                }

                put {
                    val id = uuidOrNotFoundException(call.parameters["id"])
                    val todoDraft = call.receiveOrBadRequestException<TodoDraft>()
                    val found = transaction {
                        val count = Todos.update({ Todos.id eq id }) {
                            it[description] = todoDraft.description
                        }
                        count > 0
                    }
                    found.orNotFoundException()
                    call.response.status(NoContent)
                }

                delete {
                    val id = uuidOrNotFoundException(call.parameters["id"])
                    val found = transaction {
                        val count = Todos.deleteWhere { Todos.id eq id }
                        count > 0
                    }
                    found.orNotFoundException()
                    call.response.status(NoContent)
                }
            }
        }
    }
}

@KtorExperimentalAPI
private fun Boolean.orNotFoundException() {
    if (!this) {
        throw NotFoundException()
    }
}

@KtorExperimentalAPI
private fun uuidOrNotFoundException(s: String?) =
    try {
        UUID.fromString(s)
    } catch (e: IllegalArgumentException) {
        throw NotFoundException()
    }

@KtorExperimentalAPI
suspend inline fun <reified T : Any> ApplicationCall.receiveOrBadRequestException() =
    try {
        receive(T::class)
    } catch (e: Exception) {
        throw BadRequestException("Error when receiving data from call", e)
    }
