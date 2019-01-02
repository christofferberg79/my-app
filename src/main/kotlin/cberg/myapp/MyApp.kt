package cberg.myapp

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

object Visits : Table("visit") {
    val visitedAt = datetime("visited_at")
}

//data class Visit(val visitedAt: DateTime)

fun Application.main() {
    Database.connect(System.getenv("JDBC_DATABASE_URL"), driver = "org.postgresql.Driver")

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
                    .map { rs -> rs[num] to rs[last] }.firstOrNull()
            }
            check(res != null)
            call.respondText("Number of visits: ${res.first}\nLast visit: ${res.second}")
        }
    }
}
