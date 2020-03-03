package cberg.myapp

import cberg.myapp.model.*
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.BadRequestException
import io.ktor.features.ContentNegotiation
import io.ktor.features.NotFoundException
import io.ktor.http.HttpHeaders.Location
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.receive
import io.ktor.request.uri
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.serialization.json
import io.ktor.util.KtorExperimentalAPI
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.JsonConfiguration
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.DriverManager
import java.util.*

@UnstableDefault
@KtorExperimentalAPI
fun Application.main() {
    install(ContentNegotiation) {
        json(JsonConfiguration.Default.copy(prettyPrint = true))
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
                    Todos.selectAll().map { Todo(it) }
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
                            ?.let { Todo(it) }
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

@KtorExperimentalAPI
private fun Boolean.orNotFoundException() {
    if (!this) {
        throw NotFoundException()
    }
}

@KtorExperimentalAPI
private fun String?.toUuidOrNotFoundException() =
    try {
        UUID.fromString(this)
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
