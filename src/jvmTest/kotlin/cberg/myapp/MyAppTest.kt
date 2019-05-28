package cberg.myapp

import cberg.myapp.model.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.FileSystemResourceAccessor
import org.hamcrest.Matchers.matchesPattern
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import java.sql.DriverManager
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@KtorExperimentalAPI
class AppTest {
    companion object {
        const val TODOS_PATH = "/todos"
        const val ID_PATTERN = "\\p{Graph}+"
        const val MALFORMED_ID = "invalid UUID"

        @BeforeClass
        @JvmStatic
        fun setupDatabase() {
            val dbUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
            System.setProperty("jdbcDatabaseUrl", dbUrl)

            val connection = DriverManager.getConnection(dbUrl)
            val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
            val liquibase = Liquibase("db/changelog.groovy", FileSystemResourceAccessor(), database)
            liquibase.update(Contexts())

            Database.connect(getNewConnection = { connection })
        }
    }

    @BeforeTest
    fun clearTodoTable() {
        transaction {
            Todos.deleteAll()
        }
    }

    @Test
    fun getTodosWithEmptyDatabase() = withTestApplication(Application::main) {
        // Get all Todos
        val call = handleRequest(Get, TODOS_PATH)

        // Check response
        assertEquals(OK, call.response.status())
        val todos = call.response.content?.let { jacksonObjectMapper().readValue<List<Todo>>(it) }
        assertEquals(emptyList(), todos)
    }

    @Test
    fun getTodos() = withTestApplication(Application::main) {
        // Prepare data in database
        val todo = testTodo()
        transaction {
            Todos.insert {
                it[id] = todo.id
                it[description] = todo.description
            }
        }

        // Get all Todos
        val call = handleRequest(Get, TODOS_PATH)

        // Check response
        assertEquals(OK, call.response.status())
        val todos = call.response.content?.let { jacksonObjectMapper().readValue<List<TodoWithId>>(it) }
        assertEquals(listOf(todo), todos)
    }

    @Test
    fun postTodo() = withTestApplication(Application::main) {
        // Post a Todo
        val draft = testDraft()
        val call = handleRequest(Post, TODOS_PATH) {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(jacksonObjectMapper().writeValueAsString(draft))
        }

        // Check response
        assertEquals(Created, call.response.status())
        val location = call.response.headers[HttpHeaders.Location]
        assertNotNull(location)
        assertThat(location, matchesPattern("$TODOS_PATH/$ID_PATTERN"))

        // Check data in database
        val id = UUID.fromString(location.substring("$TODOS_PATH/".length))
        val todo = transaction {
            Todos.select { Todos.id eq id }
                .map { Todo(it) }
                .single()
        }
        assertEquals(draft.description, todo.description)
    }

    @Test
    fun postTodoWithoutBody() = withTestApplication(Application::main) {
        val call = handleRequest(Post, TODOS_PATH) {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }

        assertEquals(BadRequest, call.response.status())
    }

    @Test
    fun getTodo() = withTestApplication(Application::main) {
        // Prepare data in database
        val todo = testTodo()
        transaction {
            Todos.insert {
                it[id] = todo.id
                it[description] = todo.description
            }
        }

        // Get the added Todo
        val call = handleRequest(Get, "$TODOS_PATH/${todo.id}")

        // Check response
        assertEquals(OK, call.response.status())
        val receivedTodo = call.response.content?.let { jacksonObjectMapper().readValue<TodoWithId>(it) }
        assertEquals(todo, receivedTodo)
    }

    @Test
    fun getTodoNotFound() = withTestApplication(Application::main) {
        val call = handleRequest(Get, "$TODOS_PATH/${UUID.randomUUID()}")
        assertEquals(NotFound, call.response.status())
    }

    @Test
    fun getTodoMalformedId() = withTestApplication(Application::main) {
        val call = handleRequest(Get, "$TODOS_PATH/$MALFORMED_ID")
        assertEquals(NotFound, call.response.status())
    }

    @Test
    fun deleteTodo() = withTestApplication(Application::main) {
        // Prepare data in database
        val todo = testTodo()
        transaction {
            Todos.insert {
                it[id] = todo.id
                it[description] = todo.description
            }
        }

        // Get the added Todo
        val call = handleRequest(Delete, "$TODOS_PATH/${todo.id}")

        // Check response
        assertEquals(NoContent, call.response.status())

        // Check data in database
        val deleted = transaction {
            Todos.select { Todos.id eq todo.id }.empty()
        }
        assertTrue(deleted)
    }

    @Test
    fun deleteTodoNotFound() = withTestApplication(Application::main) {
        val call = handleRequest(Delete, "$TODOS_PATH/${UUID.randomUUID()}")
        assertEquals(NotFound, call.response.status())
    }

    @Test
    fun deleteTodoMalformedId() = withTestApplication(Application::main) {
        val call = handleRequest(Delete, "$TODOS_PATH/$MALFORMED_ID")
        assertEquals(NotFound, call.response.status())
    }

    @Test
    fun putTodo() = withTestApplication(Application::main) {
        // Prepare data in database
        val todo = testTodo()
        transaction {
            Todos.insert {
                it[id] = todo.id
                it[description] = todo.description
            }
        }

        // Put a Todo update
        val draft = testDraft("test2")
        val call = handleRequest(Put, "$TODOS_PATH/${todo.id}") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(jacksonObjectMapper().writeValueAsString(draft))
        }

        // Check response
        assertEquals(NoContent, call.response.status())

        // Check data in database
        val todo2 = transaction {
            Todos.select { Todos.id eq todo.id }
                .map { Todo(it) }
                .single()
        }
        assertEquals(draft.description, todo2.description)
    }

    @Test
    fun putTodoWithoutBody() = withTestApplication(Application::main) {
        // Prepare data in database
        val todo = testTodo()
        transaction {
            Todos.insert {
                it[id] = todo.id
                it[description] = todo.description
            }
        }

        // Put a Todo update
        val call = handleRequest(Put, "$TODOS_PATH/${todo.id}") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }

        // Check response
        assertEquals(BadRequest, call.response.status())
    }

    @Test
    fun putTodoNotFound() = withTestApplication(Application::main) {
        val draft = testDraft("test2")
        val call = handleRequest(Put, "$TODOS_PATH/${UUID.randomUUID()}") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(jacksonObjectMapper().writeValueAsString(draft))
        }
        assertEquals(NotFound, call.response.status())
    }

    @Test
    fun putTodoMalformedId() = withTestApplication(Application::main) {
        val draft = testDraft("test2")
        val call = handleRequest(Put, "$TODOS_PATH/$MALFORMED_ID") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(jacksonObjectMapper().writeValueAsString(draft))
        }
        assertEquals(NotFound, call.response.status())
    }

    private fun testTodo() = testDraft().withId()
    private fun testDraft(description: String = "test") = TodoDraft(description, false)

}