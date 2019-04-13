package cberg.myapp

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.sql.DriverManager
import java.util.*

object Visits : Table("visit") {
    val visitedAt = datetime("visited_at")
}

object Todos : Table("todo") {
    val id = uuid("id")
    val description = varchar("description", 255)
}

data class Todo(val id: UUID, val description: String)

fun Application.main() {
    Database.connect(getNewConnection = {
        val url = System.getenv("JDBC_DATABASE_URL")
        DriverManager.getConnection(url)
    })

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    routing {
        get("/") {
            call.respondText("OK")
        }

        get("/db") {
            transaction {
                Visits.insert { stmt ->
                    stmt[visitedAt] = DateTime.now()
                }
            }
            val res = transaction {
                val num = Visits.visitedAt.count().alias("as num")
                val last = Visits.visitedAt.max().alias("as last")
                Visits.slice(num, last)
                    .selectAll()
                    .map { rs -> rs[num] to rs[last] }.singleOrNull()
            }
            check(res != null)
            call.respondText("Number of visits: ${res.first}\nLast visit: ${res.second}")
        }

        get("/todos") {
            transaction {
                Todos
                    .selectAll()
                    .map { Todo(it[Todos.id], it[Todos.description]) }
            }
                .let { call.respond(it) }
        }
    }
}
