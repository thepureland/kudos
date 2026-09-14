package io.kudos.ms.tag.core.runtime.job

import com.mysql.cj.jdbc.MysqlDataSource
import io.kudos.ability.data.rdb.flyway.kit.FlywayKit
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.tag.common.catalog.model.CreateTagCommand
import io.kudos.ms.tag.common.catalog.model.RegisterTagSubjectTypeCommand
import io.kudos.ms.tag.core.TagDaoTestSupport
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.catalog.service.impl.TagCatalogService
import io.kudos.ms.tag.core.catalog.subjecttype.dao.TagSubjectTypeDao
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tagset.dao.TagSetDao
import io.kudos.ms.tag.core.runtime.job.dao.TagRecalculationJobDao
import io.kudos.ms.tag.core.runtime.job.dao.TagRecalculationCandidateDao
import io.kudos.ms.tag.core.runtime.job.model.po.TagRecalculationCandidate
import io.kudos.ms.tag.core.runtime.port.RecalculationJobType
import io.kudos.ms.tag.core.runtime.port.RecalculationRequest
import io.kudos.ms.tag.core.runtime.rdb.RdbRecalculationQueue
import io.kudos.ms.tag.core.runtime.subject.dao.TagSubjectDao
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.MySqlTestContainer
import io.kudos.test.container.containers.PostgresTestContainer
import org.ktorm.database.Database
import org.ktorm.database.SqlDialect
import org.postgresql.ds.PGSimpleDataSource
import java.sql.DriverManager
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class RecalculationQueueContractTest : TagDaoTestSupport() {

    private val jobDao = TagRecalculationJobDao()
    private val clock = MutableClock(Instant.parse("2026-09-14T00:00:00Z"))
    private val retryPolicy = RecalculationRetryPolicy(Duration.ofSeconds(5), 2.0, Duration.ofSeconds(20))
    private val queue = RdbRecalculationQueue(jobDao, retryPolicy, clock)
    private lateinit var tagId: String

    @BeforeTest
    fun seedCatalog() {
        val catalog = TagCatalogService(
            TagSubjectTypeDao(),
            TagAttributeDefinitionDao(),
            TagSetDao(),
            TagDefinitionDao(),
        )
        catalog.registerSubjectType(RegisterTagSubjectTypeCommand(SUBJECT_TYPE, "Person", "hr"))
        tagId = catalog.createTag(CreateTagCommand(TENANT, SUBJECT_TYPE, "adult", "Adult")).id
    }

    @Test
    fun stableJobKeyCoalescesRequestsAndKeepsUpdatesArrivingDuringWorkRunnable() {
        val id = queue.request(request())
        assertEquals(id, queue.request(request()))
        assertEquals(2, jobDao.get(id)?.requestedVersion)

        val firstLease = queue.lease("worker-a", 1, clock.instant().plusSeconds(30)).single()
        assertEquals(2, firstLease.requestedVersion)
        assertEquals(id, queue.request(request()))
        assertEquals("RUNNING", jobDao.get(id)?.status)

        assertTrue(queue.complete(id, "worker-a", firstLease.version, firstLease.requestedVersion, 7))
        val pending = requireNotNull(jobDao.get(id))
        assertEquals("PENDING", pending.status)
        assertEquals(2, pending.processedVersion)
        assertEquals(3, pending.requestedVersion)

        val secondLease = queue.lease("worker-b", 1, clock.instant().plusSeconds(30)).single()
        assertTrue(queue.complete(id, "worker-b", secondLease.version, secondLease.requestedVersion, 3))
        assertEquals("SUCCEEDED", jobDao.get(id)?.status)
    }

    @Test
    fun onlyOneWorkerLeasesAJobAndExpiredLeaseIsFencedAfterReclaim() {
        val id = queue.request(request())
        val oldLease = queue.lease("worker-a", 1, clock.instant().plusSeconds(10)).single()

        assertTrue(queue.lease("worker-b", 1, clock.instant().plusSeconds(10)).isEmpty())
        clock.advance(Duration.ofSeconds(11))
        val newLease = queue.lease("worker-b", 1, clock.instant().plusSeconds(10)).single()

        assertNotEquals(oldLease.version, newLease.version)
        assertFalse(queue.complete(id, "worker-a", oldLease.version, oldLease.requestedVersion, 1))
        assertTrue(queue.complete(id, "worker-b", newLease.version, newLease.requestedVersion, 1))
    }

    @Test
    fun retriesWithCappedBackoffAndFailsAtMaximumAttempts() {
        assertEquals(Duration.ofSeconds(5), retryPolicy.delayForAttempt(1))
        assertEquals(Duration.ofSeconds(10), retryPolicy.delayForAttempt(2))
        assertEquals(Duration.ofSeconds(20), retryPolicy.delayForAttempt(10))

        val id = queue.request(request())
        requireNotNull(jobDao.get(id)).also {
            it.maxAttempts = 2
            check(jobDao.update(it))
        }
        val first = queue.lease("worker", 1, clock.instant().plusSeconds(10)).single()
        assertTrue(queue.fail(id, "worker", first.version, "TEMPORARY", "try later"))
        assertEquals("RETRY_WAIT", jobDao.get(id)?.status)
        assertTrue(queue.lease("worker", 1, clock.instant().plusSeconds(10)).isEmpty())

        clock.advance(Duration.ofSeconds(5))
        val second = queue.lease("worker", 1, clock.instant().plusSeconds(10)).single()
        assertTrue(queue.fail(id, "worker", second.version, "TEMPORARY", "still unavailable"))
        assertEquals("FAILED", jobDao.get(id)?.status)
        assertTrue(queue.lease("worker", 1, clock.instant().plusSeconds(10)).isEmpty())
    }

    @Test
    fun crashedFinalAttemptBecomesFailedWhenItsLeaseExpires() {
        val id = queue.request(request())
        requireNotNull(jobDao.get(id)).also {
            it.maxAttempts = 1
            check(jobDao.update(it))
        }
        queue.lease("crashed-worker", 1, clock.instant().plusSeconds(5)).single()

        clock.advance(Duration.ofSeconds(6))

        assertTrue(queue.lease("next-worker", 1, clock.instant().plusSeconds(5)).isEmpty())
        assertEquals("FAILED", jobDao.get(id)?.status)
        assertEquals("LEASE_EXPIRED_MAX_ATTEMPTS", jobDao.get(id)?.lastErrorCode)
    }

    @Test
    fun cancellationFencesAStaleWorker() {
        val id = queue.request(request())
        val lease = queue.lease("worker", 1, clock.instant().plusSeconds(30)).single()

        assertTrue(queue.cancel(id))
        assertFalse(queue.complete(id, "worker", lease.version, lease.requestedVersion, 1))
        assertEquals("CANCELLED", jobDao.get(id)?.status)
    }

    @Test
    @EnabledIfDockerInstalled
    fun mysqlExecutesSkipLockedLease() {
        val container = MySqlTestContainer.startIfNeeded(null)
        val host = container.ports.first().ip
        val port = requireNotNull(container.ports.first().publicPort)
        val databaseName = "tag_queue_${suffix()}"
        val adminUrl = "jdbc:mysql://$host:$port/?useSSL=false&allowPublicKeyRetrieval=true"
        DriverManager.getConnection(adminUrl, MySqlTestContainer.USERNAME, MySqlTestContainer.PASSWORD).use {
            it.createStatement().use { statement -> statement.executeUpdate("create database $databaseName") }
        }
        try {
            verifyDialect(MysqlDataSource().apply {
                setURL("jdbc:mysql://$host:$port/$databaseName?useSSL=false&allowPublicKeyRetrieval=true")
                user = MySqlTestContainer.USERNAME
                password = MySqlTestContainer.PASSWORD
            })
        } finally {
            KudosContextHolder.clear()
            DriverManager.getConnection(adminUrl, MySqlTestContainer.USERNAME, MySqlTestContainer.PASSWORD).use {
                it.createStatement().use { statement -> statement.executeUpdate("drop database $databaseName") }
            }
        }
    }

    @Test
    @EnabledIfDockerInstalled
    fun postgresqlExecutesSkipLockedLease() {
        val databaseName = "tag_queue_${suffix()}"
        val container = PostgresTestContainer.startIfNeeded(null, databaseName)
        verifyDialect(PGSimpleDataSource().apply {
            serverNames = arrayOf(container.ports.first().ip)
            portNumbers = intArrayOf(requireNotNull(container.ports.first().publicPort))
            this.databaseName = databaseName
            user = PostgresTestContainer.USERNAME
            password = PostgresTestContainer.PASSWORD
        })
    }

    private fun verifyDialect(dataSource: DataSource) {
        KudosContextHolder.clear()
        FlywayKit.migrate("tag", dataSource)
        val dialectTagId = "00000000-0000-0000-0000-000000000099"
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """insert into tag_subject_type
                       (id, code, name, owner_service_code, active, built_in, version, create_time, update_time)
                       values ('00000000-0000-0000-0000-000000000001', '$SUBJECT_TYPE', 'Person', 'hr', true, false, 0, current_timestamp, current_timestamp)""".trimIndent()
                )
                statement.executeUpdate(
                    """insert into tag_subject
                       (subject_id, tenant_id, subject_type, state_version, first_seen_time, update_time)
                       values ('person-a', '$TENANT', '$SUBJECT_TYPE', 0, current_timestamp, current_timestamp),
                              ('person-b', '$TENANT', '$SUBJECT_TYPE', 0, current_timestamp, current_timestamp)""".trimIndent()
                )
                statement.executeUpdate(
                    """insert into tag_definition
                       (id, tenant_id, subject_type, code, name, set_priority, manual_assignable, active, built_in, version, create_time, update_time)
                       values ('$dialectTagId', '$TENANT', '$SUBJECT_TYPE', 'dialect', 'Dialect', 0, true, true, false, 0, current_timestamp, current_timestamp)""".trimIndent()
                )
            }
        }
        KudosContextHolder.get().addOtherInfos(
            KudosContext.OTHER_INFO_KEY_DATABASE to Database.connect(dataSource, dialect = object : SqlDialect {})
        )
        val dialectQueue = RdbRecalculationQueue(
            TagRecalculationJobDao(),
            clock = Clock.fixed(clock.instant(), ZoneId.of("UTC")),
        )
        val id = dialectQueue.request(
            request().copy(tagId = dialectTagId, subjectId = "dialect-subject")
        )
        val lease = dialectQueue.lease("dialect-worker", 1, clock.instant().plusSeconds(30)).single()

        assertTrue(dialectQueue.complete(id, "dialect-worker", lease.version, lease.requestedVersion, 1))

        val fullId = dialectQueue.request(
            request().copy(
                jobType = RecalculationJobType.RULE_FULL_REBUILD,
                tagId = dialectTagId,
                subjectId = null,
            )
        )
        val fullLease = dialectQueue.lease("rebuild-worker", 1, clock.instant().plusSeconds(30)).single()
        assertEquals(listOf("person-a"), TagSubjectDao().listKeysAfter(TENANT, SUBJECT_TYPE, null, 1).map { it.subjectId })
        assertTrue(
            TagRecalculationJobDao().advanceFullRebuild(
                fullId,
                "rebuild-worker",
                "person-a",
                1,
                exhausted = false,
                LocalDateTime.ofInstant(clock.instant(), ZoneId.of("UTC")),
            )
        )
        assertEquals("PENDING", TagRecalculationJobDao().get(fullLease.id)?.status)
        val candidateDao = TagRecalculationCandidateDao()
        assertTrue(candidateDao.insertCandidate(TagRecalculationCandidate().apply {
            runId = fullId
            tenantId = TENANT
            subjectType = SUBJECT_TYPE
            subjectId = "person-a"
            tagId = dialectTagId
            ruleVersion = 1
            evaluatedTime = LocalDateTime.ofInstant(clock.instant(), ZoneId.of("UTC"))
        }))
        assertEquals(listOf("person-a"), candidateDao.listByRun(fullId).map { it.subjectId })
    }

    private fun suffix() = UUID.randomUUID().toString().replace("-", "")

    private fun request() = RecalculationRequest(
        tenantId = TENANT,
        jobType = RecalculationJobType.SUBJECT_INCREMENTAL,
        tagId = tagId,
        ruleVersion = 1,
        subjectType = SUBJECT_TYPE,
        subjectId = "person-1",
    )

    private class MutableClock(private var now: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("UTC")
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = now
        fun advance(duration: Duration) {
            now = now.plus(duration)
        }
    }

    private companion object {
        const val TENANT = "tenant-a"
        const val SUBJECT_TYPE = "hr.person"
    }
}
