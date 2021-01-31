package failfast.r2dbc

import failfast.ContextDSL
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager
import java.util.*

object TestConfig {
    val ALL_PSQL = System.getenv("ALL_PSQL") != null
    val H2_ONLY = System.getenv("H2_ONLY") != null
}

open class DBTestUtil(val databaseName: String) {
    interface TestDatabase {
        val name: String

        fun createDB(): ConnectionFactory
        fun prepare() {}
    }

    inner class H2TestDatabase : TestDatabase {
        override val name = "H2"

        override fun createDB(): ConnectionFactory {
            val uuid = UUID.randomUUID()
            val databaseName = "$databaseName$uuid"
            val jdbcUrl = "jdbc:h2:mem:$databaseName;DB_CLOSE_DELAY=-1"
            val flyway = Flyway.configure().dataSource(jdbcUrl, "", "").load()
            flyway.migrate()
            return ConnectionFactories.get("r2dbc:h2:mem:///$databaseName;DB_CLOSE_DELAY=-1")
        }
    }


    inner class PSQLTestDatabase(private val dockerImage: String) : TestDatabase {
        override val name = dockerImage

        override fun prepare() {
            dockerContainer
        }

        private val dockerContainer: PostgreSQLContainer<Nothing> by
        lazy {
            PostgreSQLContainer<Nothing>(dockerImage).apply {
// WIP           setCommand("postgres", "-c", "fsync=off", "-c", "max_connections=200")
                withReuse(true)
                start()
            }
        }

        override fun createDB(): ConnectionFactory {
            val (databaseName, host, port) = preparePostgresDB()
            return ConnectionFactories.get("r2dbc:pool:postgresql://test:test@$host:$port/$databaseName?initialSize=1")
        }

        fun preparePostgresDB(): NameHostAndPort {
            Class.forName("org.postgresql.Driver")
            val uuid = UUID.randomUUID()
            val databaseName = "$databaseName$uuid".replace("-", "_")
            // testcontainers says that it returns an ip address but it returns a host name.
            val host = dockerContainer.containerIpAddress.let { if (it == "localhost") "127.0.0.1" else it }
            val port = dockerContainer.getMappedPort(5432)
            val db =
                DriverManager.getConnection("jdbc:postgresql://$host:$port/postgres", "test", "test")
            @Suppress("SqlNoDataSourceInspection")
            db.createStatement().executeUpdate("create database $databaseName")
            db.close()

            val flyway =
                Flyway.configure()
                    .dataSource("jdbc:postgresql://$host:$port/$databaseName", "test", "test")
                    .load()
            flyway.migrate()
            return NameHostAndPort(databaseName, host, port)
        }
    }

    data class NameHostAndPort(val databaseName: String, val host: String, val port: Int)

    val h2 = H2TestDatabase()
    val psql13 = PSQLTestDatabase("postgres:13-alpine")
    val databases = when {
        TestConfig.H2_ONLY -> {
            listOf(h2)
        }
        TestConfig.ALL_PSQL -> {
            listOf(
                h2, psql13,
                PSQLTestDatabase("postgres:12-alpine"),
                PSQLTestDatabase("postgres:11-alpine"),
                PSQLTestDatabase("postgres:10-alpine"),
                PSQLTestDatabase("postgres:9-alpine")
            )
        }
        else -> listOf(h2, psql13)
    }


}

suspend fun ContextDSL.forAllDatabases(dbs: DBTestUtil, tests: suspend ContextDSL.(ConnectionFactory) -> Unit) {
    dbs.databases.map { db ->
        context("on ${db.name}") {
            val connectionFactory = db.createDB()
            tests(connectionFactory)
        }
    }
}

