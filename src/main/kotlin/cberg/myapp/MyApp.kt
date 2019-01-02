package cberg.myapp

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing

fun Application.main() {
    routing {
        get("/") {
            call.respondText("OK")
        }

        get("/db") {
            call.respondText(System.getenv("DATABASE_URL"))
        }
    }
}
