package cberg.myapp

import io.ktor.application.Application
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class AppTest {
//    @Test
//    fun testApp() = withTestApplication(Application::main) {
//        with(handleRequest(Get, "/")) {
//            assertEquals(OK, response.status())
//            assertEquals("OK", response.content)
//        }
//    }
}