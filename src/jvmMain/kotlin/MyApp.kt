package cberg.myapp

import cberg.myapp.model.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.HttpHeaders.Location
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.DriverManager
import java.util.*

fun Application.main() {
    install(ContentNegotiation) {
        json(Json { prettyPrint = true })
    }

    Database.connect(getNewConnection = {
        val url = System.getProperty("jdbcDatabaseUrl")
        DriverManager.getConnection(url)
    })

    routing {
        static("/") {
            resources("web")
            defaultResource("web/index.html")
        }

        route("todos") {
            get {
                val todos = transaction {
                    Todos.selectAll().map { TodoWithId(it) }
                }
                call.respond(todos)
            }

            post {
                val draft = call.receiveOrBadRequestException<TodoDraft>()
                val todo = draft.withId()
                transaction {
                    Todos.insert { statement ->
                        set(statement, todo)
                    }
                }
                call.response.status(Created)
                call.response.header(Location, "${call.request.uri}/${todo.id}")
            }

            route("{id}") {
                get {
                    val id = call.parameters["id"].toUuidOrNotFoundException()
                    val todo = transaction {
                        Todos.select { Todos.id eq id }.singleOrNull()
                            ?.let { TodoWithId(it) }
                            ?: throw NotFoundException()
                    }
                    call.respond(todo)
                }

                put {
                    val id = call.parameters["id"].toUuidOrNotFoundException()
                    val draft = call.receiveOrBadRequestException<TodoDraft>()
                    val found = transaction {
                        val count = Todos.update(where = { Todos.id eq id }) { statement ->
                            set(statement, draft)
                        }
                        count > 0
                    }
                    found.orNotFoundException()
                    call.response.status(NoContent)
                }

                delete {
                    val id = call.parameters["id"].toUuidOrNotFoundException()
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

private fun Boolean.orNotFoundException() {
    if (!this) {
        throw NotFoundException()
    }
}

private fun String?.toUuidOrNotFoundException() =
    try {
        UUID.fromString(this)
    } catch (e: IllegalArgumentException) {
        throw NotFoundException()
    }

suspend inline fun <reified T : Any> ApplicationCall.receiveOrBadRequestException() =
    try {
        receive(T::class)
    } catch (e: Exception) {
        throw BadRequestException("Error when receiving data from call", e)
    }
