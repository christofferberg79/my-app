package cberg.myapp

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.hamcrest.Matchers.matchesPattern
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import java.sql.DriverManager
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class AppTest {
    companion object {
        @ClassRule
        @JvmField
        val envVars = EnvironmentVariables()

        const val TODOS_PATH = "/todos"
        const val ID_PATTERN = "\\p{Graph}+"

        @BeforeClass
        @JvmStatic
        fun setupDatabase() {
            val dbUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
            envVars.set("JDBC_DATABASE_URL", dbUrl)

            val connection = DriverManager.getConnection(dbUrl)
            val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
            val liquibase = Liquibase("db/changelog.groovy", ClassLoaderResourceAccessor(), database)
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
    fun getAllTodosWithEmptyDatabase() = withTestApplication(Application::main) {
        // Get all Todos
        val call = handleRequest(Get, TODOS_PATH)

        // Check response
        assertEquals(OK, call.response.status())
        val todos = call.response.content?.let { jacksonObjectMapper().readValue<List<Todo>>(it) }
        assertEquals(emptyList(), todos)
    }

    @Test
    fun getAllTodosWithSingleEntry() = withTestApplication(Application::main) {
        // Prepare data in database
        val todo = Todo(UUID.randomUUID(), "test")
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
        val todos = call.response.content?.let { jacksonObjectMapper().readValue<List<Todo>>(it) }
        assertEquals(listOf(todo), todos)
    }

    @Test
    fun postTodo() = withTestApplication(Application::main) {
        // Post a Todo
        val todoDraft = TodoDraft("test")
        val call = handleRequest(Post, TODOS_PATH) {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(jacksonObjectMapper().writeValueAsString(todoDraft))
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
                .map { Todo(it[Todos.id], it[Todos.description]) }
                .single()
        }
        assertEquals(todoDraft.description, todo.description)
    }

    @Test
    fun getSingleTodo() = withTestApplication(Application::main) {
        // Prepare data in database
        val todo = Todo(UUID.randomUUID(), "test")
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
        val receivedTodo = call.response.content?.let { jacksonObjectMapper().readValue<Todo>(it) }
        assertEquals(todo, receivedTodo)
    }

    @Test
    fun deleteTodo() = withTestApplication(Application::main) {
        // Prepare data in database
        val todo = Todo(UUID.randomUUID(), "test")
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

}