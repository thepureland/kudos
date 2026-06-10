package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.main.ManualTestContainerMainSupport
import io.kudos.test.container.support.TestContainerCrossProcessLock
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

/**
 * postgres test container.
 *
 * To persist database files to the host, set the system property [SYS_PROP_HOST_DATA_DIR] or environment variable [ENV_HOST_DATA_DIR];
 * that path is bound to `/var/lib/postgresql/data` inside the container (read-write). When unset, data is stored only in the container layer, matching the previous behavior.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
object PostgresTestContainer {

    /** Host data directory; takes precedence over [ENV_HOST_DATA_DIR]. */
    const val SYS_PROP_HOST_DATA_DIR = "kudos.test.postgres.data.dir"

    /** Host data directory (used when the system property is unset). */
    const val ENV_HOST_DATA_DIR = "KUDOS_TEST_POSTGRES_DATA_DIR"

    /** Default PGDATA path in the official image. */
    private const val CONTAINER_PGDATA = "/var/lib/postgresql/data"

    private const val LOCALHOST = "127.0.0.1"

    private const val DATABASE_READY_MAX_RETRIES = 20

    private const val DATABASE_READY_RETRY_INTERVAL_MILLIS = 500L

    private const val IMAGE_NAME = "postgres:18.0-alpine3.22"

    const val DATABASE = "test"

    /**
     * The host-side mapped port of the postgres container. **Only available after the container has started** — earlier calls throw.
     *
     * No longer fixed at 25432: testcontainers asks Docker for a dynamic port, avoiding test startup failures when another local process
     * (e.g. a leftover postgres container from another project) is holding a fixed port. All external callers should obtain the port via this getter;
     * any port specified in yml/CLI args is only a fallback and will be overridden at runtime by [registerProperties] / CLI `--spring.datasource...url`.
     */
    val PORT: Int
        get() {
            val running = TestContainerKit.getRunningContainer(LABEL)
                ?: error("PostgresTestContainer.PORT requested before container start; call startIfNeeded(...) first.")
            return running.ports.firstOrNull()?.publicPort
                ?: error("Postgres container is up but exposes no public port mapping.")
        }

    const val CONTAINER_PORT = 5432

    const val USERNAME = "pg"

    const val PASSWORD = "postgres"

    const val LABEL = "PostgreSql"

    private var container: GenericContainer<*>? = null

    /**
     * Starts the postgres container using the default database name.
     *
     * @param registry the Spring dynamic property registry, used to register or override already-registered properties; may be {@code null}
     * @return information about the currently running postgres container
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        return startIfNeeded(registry, DATABASE)
    }

    /**
     * Starts the postgres container and ensures the given database exists.
     * <p>
     * If the container has not been started, it is started first; if it is already running, the database is created inside the existing container.
     *
     * @param registry the Spring dynamic property registry, used to register or override already-registered properties; may be {@code null}
     * @param database the database name to ensure exists
     * @return information about the currently running postgres container
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?, database: String): Container {
        return TestContainerCrossProcessLock.run(PostgresTestContainer::class.java, "postgres") {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, getOrCreateContainer())
            ensureDatabaseExists(runningContainer, database)
            if (registry != null) {
                registerProperties(registry, runningContainer, database)
            }
            runningContainer
        }
    }

    /**
     * Lazily creates the container definition to avoid fixing container state at object initialization time.
     *
     * @return the postgres container definition
     */
    private fun getOrCreateContainer(): GenericContainer<*> {
        if (container == null) {
            container = GenericContainer(IMAGE_NAME).apply {
                // Do not call bindingPort — let testcontainers auto-assign a host port. With a fixed 25432,
                // if another local postgres process holds that port (e.g. a leftover container from another project), the
                // whole test would fail at startup with "port is already allocated".
                withExposedPorts(CONTAINER_PORT)
                withEnv("POSTGRES_DB", DATABASE)
                withEnv("POSTGRES_USER", USERNAME)
                withEnv("POSTGRES_PASSWORD", PASSWORD)
                withCommand("postgres -c max_prepared_transactions=10")
                withLabel(TestContainerKit.LABEL_KEY, LABEL)
                resolveHostDataDir()?.let { hostPath ->
                    Files.createDirectories(Paths.get(hostPath))
                    withFileSystemBind(hostPath, CONTAINER_PGDATA, BindMode.READ_WRITE)
                }
            }
        }
        return requireNotNull(container)
    }

    /**
     * Resolves the host-side directory used to persist PGDATA.
     *
     * @return the path when a non-empty absolute or canonical path is configured; `null` when unconfigured (no volume mounted)
     */
    private fun resolveHostDataDir(): String? {
        val fromProp = System.getProperty(SYS_PROP_HOST_DATA_DIR)?.trim().orEmpty()
        if (fromProp.isNotEmpty()) {
            return fromProp
        }
        val fromEnv = System.getenv(ENV_HOST_DATA_DIR)?.trim().orEmpty()
        return fromEnv.takeIf { it.isNotEmpty() }
    }

    /**
     * Connects to the admin database {@code postgres} and creates the target database on demand.
     */
    private fun ensureDatabaseExists(runningContainer: Container, database: String) {
        validateDatabaseName(database)
        val url = buildAdminJdbcUrl(runningContainer)
        var lastException: SQLException? = null
        repeat(DATABASE_READY_MAX_RETRIES) { attempt ->
            try {
                DriverManager.getConnection(url, USERNAME, PASSWORD).use { connection ->
                    if (databaseExists(connection, database)) {
                        return
                    }
                    connection.createStatement().use { statement ->
                        statement.execute("""create database "${database.replace("\"", "\"\"")}"""")
                    }
                    return
                }
            } catch (e: SQLException) {
                lastException = e
                if (attempt == DATABASE_READY_MAX_RETRIES - 1) {
                    return@repeat
                }
                sleepBeforeRetry()
            }
        }
        throw RuntimeException("failed to ensure postgres database exists: $database", lastException)
    }

    /**
     * Builds the JDBC URL used to connect to the admin database.
     *
     * @param runningContainer the currently running container
     * @return the JDBC URL pointing to the postgres admin database
     */
    private fun buildAdminJdbcUrl(runningContainer: Container): String {
        val port = runningContainer.ports.firstOrNull()?.publicPort
            ?: error("Postgres container has no public port mapping; cannot connect to admin DB.")
        return "jdbc:postgresql://$LOCALHOST:$port/postgres?sslmode=disable"
    }

    /**
     * Checks whether the target database already exists.
     */
    private fun databaseExists(connection: Connection, database: String): Boolean {
        val sql = "select 1 from pg_database where datname = ?"
        connection.prepareStatement(sql).use { statement: PreparedStatement ->
            statement.setString(1, database)
            statement.executeQuery().use { resultSet: ResultSet ->
                return resultSet.next()
            }
        }
    }

    /**
     * Restricts the character set of database names to avoid the risks of empty or invalid identifiers during creation.
     */
    private fun validateDatabaseName(database: String) {
        require(database.isNotBlank()) { "database must not be blank" }
        require(database.matches(Regex("[A-Za-z0-9_]+"))) {
            "database contains unsupported characters: $database"
        }
    }

    /**
     * Sleeps briefly before retrying when the database is not yet ready.
     */
    private fun sleepBeforeRetry() {
        try {
            Thread.sleep(DATABASE_READY_RETRY_INTERVAL_MILLIS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("interrupted while waiting for postgres to become ready", e)
        }
    }

    /**
     * Registers data-source properties corresponding to the specified database.
     */
    private fun registerProperties(
        registry: DynamicPropertyRegistry,
        runningContainer: Container,
        database: String
    ) {
        val host = runningContainer.ports.first().ip
        val port = runningContainer.ports.first().publicPort

        val url = "jdbc:postgresql://$host:$port/$database"
        registry.add("spring.datasource.dynamic.datasource.postgres.url") { url }
        registry.add("spring.datasource.dynamic.datasource.postgres.username") { USERNAME }
        registry.add("spring.datasource.dynamic.datasource.postgres.password") { PASSWORD }
    }

    /**
     * Returns the running container instance.
     *
     * @return the container instance, or {@code null} if none is running
     */
    fun getRunningContainer(): Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        ManualTestContainerMainSupport.removeExistingContainers(LABEL, "Postgres")
        startIfNeeded(null)
        println("postgres localhost port: $PORT")
        Thread.sleep(Long.MAX_VALUE)
    }

}
