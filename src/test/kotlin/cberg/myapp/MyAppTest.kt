package cberg.myapp

import io.ktor.application.Application
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals


class AppTest {
    companion object {
        @ClassRule
        @JvmField
        val envVars = EnvironmentVariables()

        lateinit var connection: Connection

        @BeforeClass
        @JvmStatic
        fun setupDatabase() {
            val dbUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
            envVars.set("JDBC_DATABASE_URL", dbUrl)

            connection = DriverManager.getConnection(dbUrl)
            val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
            val liquibase = Liquibase("db/changelog.groovy", ClassLoaderResourceAccessor(), database)
            liquibase.update(Contexts())
        }
    }

    @Test
    fun testRoot() = withTestApplication(Application::main) {
        with(handleRequest(Get, "/")) {
            assertEquals(OK, response.status())
            assertEquals("OK", response.content)
        }
    }

    @Test
    fun testDb() = withTestApplication(Application::main) {
        with(handleRequest(Get, "/db")) {
            assertEquals(OK, response.status())
            assertThat(response.content, startsWith("Number of visits: "))
        }
    }

    @Test
    fun testEmptyTodos() = withTestApplication(Application::main) {
        connection.run {
            createStatement().executeUpdate("DELETE FROM todo")
            commit()
        }
        with(handleRequest(Get, "/todos")) {
            assertEquals(OK, response.status())
            assertThat(response.content, matchesPattern("\\[\\s*\\]"))
        }
    }

    @Test
    fun testSingletonTodos() = withTestApplication(Application::main) {
        connection.run {
            createStatement().executeUpdate("DELETE FROM todo")
            createStatement().executeUpdate("INSERT INTO todo VALUES('123', 'test')")
            commit()
        }
        with(handleRequest(Get, "/todos")) {
            assertEquals(OK, response.status())
            assertThat(response.content, containsString("123"))
            assertThat(response.content, containsString("test"))
        }
    }

}