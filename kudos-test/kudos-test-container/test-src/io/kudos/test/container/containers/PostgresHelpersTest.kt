package io.kudos.test.container.containers

import io.kudos.test.container.containers.ContainerPortMocks.container
import io.kudos.test.container.containers.ContainerPortMocks.port
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers PostgresTestContainer pure helpers: database-name validation, host-data-dir resolution
 * (system property precedence over env, blank handling), and admin JDBC URL building including its
 * no-port guard.
 *
 * @author K
 * @since 1.0.0
 */
internal class PostgresHelpersTest {

    @AfterTest
    fun cleanup() {
        System.clearProperty(PostgresTestContainer.SYS_PROP_HOST_DATA_DIR)
    }

    @Test
    fun `validateDatabaseName accepts alnum and underscore`() {
        PostgresTestContainer.validateDatabaseName("test")
        PostgresTestContainer.validateDatabaseName("My_DB_123")
    }

    @Test
    fun `validateDatabaseName rejects blank`() {
        val ex = assertFailsWith<IllegalArgumentException> { PostgresTestContainer.validateDatabaseName("   ") }
        assertEquals("database must not be blank", ex.message)
    }

    @Test
    fun `validateDatabaseName rejects unsupported characters`() {
        val ex = assertFailsWith<IllegalArgumentException> { PostgresTestContainer.validateDatabaseName("bad-name") }
        assertTrue(ex.message!!.contains("unsupported characters"))
        assertFailsWith<IllegalArgumentException> { PostgresTestContainer.validateDatabaseName("a;b") }
        assertFailsWith<IllegalArgumentException> { PostgresTestContainer.validateDatabaseName("名字") }
    }

    @Test
    fun `resolveHostDataDir returns null when unset`() {
        System.clearProperty(PostgresTestContainer.SYS_PROP_HOST_DATA_DIR)
        // env var is not set on the build agent; with no property either, expect null.
        // (If the env var happens to be set, this assertion is skipped to stay deterministic.)
        if (System.getenv(PostgresTestContainer.ENV_HOST_DATA_DIR).isNullOrBlank()) {
            assertNull(PostgresTestContainer.resolveHostDataDir())
        }
    }

    @Test
    fun `resolveHostDataDir prefers system property and trims`() {
        val dir = Files.createTempDirectory("pg-data").toString()
        System.setProperty(PostgresTestContainer.SYS_PROP_HOST_DATA_DIR, "  $dir  ")
        assertEquals(dir, PostgresTestContainer.resolveHostDataDir())
    }

    @Test
    fun `resolveHostDataDir ignores blank system property`() {
        System.setProperty(PostgresTestContainer.SYS_PROP_HOST_DATA_DIR, "   ")
        // Falls through to env; on the agent env is unset so result is null.
        if (System.getenv(PostgresTestContainer.ENV_HOST_DATA_DIR).isNullOrBlank()) {
            assertNull(PostgresTestContainer.resolveHostDataDir())
        }
    }

    @Test
    fun `buildAdminJdbcUrl points at postgres admin db on localhost`() {
        val url = PostgresTestContainer.buildAdminJdbcUrl(container(ports = arrayOf(port("ignored", 15999))))
        assertEquals("jdbc:postgresql://127.0.0.1:15999/postgres?sslmode=disable", url)
    }

    @Test
    fun `buildAdminJdbcUrl fails without public port mapping`() {
        val ex = assertFailsWith<IllegalStateException> {
            PostgresTestContainer.buildAdminJdbcUrl(container(ports = arrayOf(port("x", null))))
        }
        assertTrue(ex.message!!.contains("no public port mapping"))
    }

    @Test
    fun `buildAdminJdbcUrl fails when no ports at all`() {
        assertFailsWith<IllegalStateException> {
            PostgresTestContainer.buildAdminJdbcUrl(container())
        }
    }

    @Test
    fun `getOrCreateContainer builds and caches a single container without data dir`() {
        System.clearProperty(PostgresTestContainer.SYS_PROP_HOST_DATA_DIR)
        val first = PostgresTestContainer.getOrCreateContainer()
        val second = PostgresTestContainer.getOrCreateContainer()
        // lazy singleton: the same instance is returned on the second call (cache branch)
        assertTrue(first === second)
        assertTrue(first.exposedPorts.contains(PostgresTestContainer.CONTAINER_PORT))
    }

    @Test
    fun `sleepBeforeRetry rethrows when interrupted`() {
        Thread.currentThread().interrupt()
        val ex = assertFailsWith<RuntimeException> { PostgresTestContainer.sleepBeforeRetry() }
        assertTrue(ex.message!!.contains("interrupted"))
        assertTrue(ex.cause is InterruptedException)
        // interrupt flag must have been re-set by the handler
        assertTrue(Thread.interrupted(), "interrupt status should be restored (and is cleared here)")
    }

    @Test
    fun `sleepBeforeRetry sleeps normally when not interrupted`() {
        // clear any stray interrupt from other tests, then a normal short sleep must not throw
        Thread.interrupted()
        PostgresTestContainer.sleepBeforeRetry()
    }

    @Test
    fun `PORT getter fails when container not started`() {
        // No postgres container is running in this unit scope -> the getter must fail fast with
        // IllegalStateException (either the "before container start" guard, or, on a Docker-less
        // agent, the upstream ensureDockerRunning failure — both are IllegalStateException).
        assertFailsWith<IllegalStateException> { PostgresTestContainer.PORT }
    }
}
