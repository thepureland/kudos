package io.kudos.test.container.containers

import io.kudos.test.container.FakeDynamicPropertyRegistry
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.main.ManualTestContainerMainSupport
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Docker-gated integration coverage for [PostgresTestContainer]'s start + JDBC bootstrap path:
 * `startIfNeeded` (default db + extra db), `getOrCreateContainer` real start, `ensureDatabaseExists`
 * / `databaseExists` (existing db short-circuit + create-new branch), the [PostgresTestContainer.PORT]
 * getter success path, and `registerProperties` against the live container.
 *
 * Uses the small `postgres:*-alpine` image. Runs only when Docker is installed.
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
internal class PostgresDockerIT {

    @Test
    fun `postgres start, ensure databases, register props`() {
        ManualTestContainerMainSupport.removeExistingContainers(PostgresTestContainer.LABEL, "Postgres")
        try {
            val reg = FakeDynamicPropertyRegistry()
            // first start ensures the default database exists (create-new branch via fresh container)
            val running = PostgresTestContainer.startIfNeeded(reg)
            assertNotNull(running)
            assertTrue(TestContainerKit.isContainerRunning(PostgresTestContainer.LABEL))

            // PORT getter success path (container is up)
            assertTrue(PostgresTestContainer.PORT > 0)

            // default db url registered
            assertTrue(reg.has("spring.datasource.dynamic.datasource.postgres.url"))

            // second call with the SAME default db hits the databaseExists() == true short-circuit
            PostgresTestContainer.startIfNeeded(null)

            // a brand-new db name hits the create-database branch
            val reg2 = FakeDynamicPropertyRegistry()
            PostgresTestContainer.startIfNeeded(reg2, "kudos_extra_db")
            val url = reg2.value("spring.datasource.dynamic.datasource.postgres.url") as String
            assertTrue(url.endsWith("/kudos_extra_db"))
        } finally {
            runCatching { TestContainerKit.stopContainerByLabel(PostgresTestContainer.LABEL) }
            runCatching { ManualTestContainerMainSupport.removeExistingContainers(PostgresTestContainer.LABEL, "Postgres") }
        }
    }
}
