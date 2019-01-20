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
import org.hamcrest.Matchers.startsWith
import org.junit.Assert.assertThat
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals


class AppTest {
    companion object {
        @ClassRule
        @JvmField
        val envVars = EnvironmentVariables()

        @BeforeClass
        @JvmStatic
        fun setupDatabase() {
            val dbUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
            val dbDriver = "org.h2.Driver"
            envVars.set("JDBC_DATABASE_URL", dbUrl)
            envVars.set("JDBC_DATABASE_DRIVER", dbDriver)

            val connection = DriverManager.getConnection(dbUrl)
            val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
            val liquibase = Liquibase("db/changelog.xml", ClassLoaderResourceAccessor(), database)
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

}