package io.kudos.ms.auth.core.authentication.mfa.policy

import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.dao.AuthMfaEnrollmentExemptionDao
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.MfaEnrollmentExemptionRevokeCommand
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.po.AuthMfaEnrollmentExemption
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.service.iservice.IMfaEnrollmentExemptionService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Real Flyway/H2 regression for the V57 exemption table: what counts as active, what revocation writes, and
 * the constraints that keep a grant from being recorded in a state nobody could explain later.
 */
@EnabledIfDockerInstalled
internal class AuthMfaEnrollmentExemptionDaoTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var dao: AuthMfaEnrollmentExemptionDao

    @Resource
    private lateinit var service: IMfaEnrollmentExemptionService

    @Test
    fun v57ReadsOnlyUnexpiredActiveGrantsAndRevokesEveryOneOfThem() {
        val tenantId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        dao.insert(exemption(tenantId, userId, expires = NOW.plusDays(1)))
        dao.insert(exemption(tenantId, userId, expires = NOW.plusDays(4)))
        dao.insert(exemption(tenantId, userId, expires = NOW.minusDays(1), granted = NOW.minusDays(3)))
        dao.insert(
            exemption(tenantId, userId, expires = NOW.plusDays(9)).apply {
                status = "REVOKED"
                revokedBy = "admin-9"
                revokeReason = "granted by mistake"
                revokedAt = NOW
            }
        )

        // Expired and already-revoked grants are simply not there; the effective window is the latest active one.
        assertEquals(
            listOf(NOW.plusDays(4), NOW.plusDays(1)),
            dao.findActive(tenantId, userId, NOW).map { it.expiresAt },
        )
        assertEquals(NOW.plusDays(4), service.activeExemptionExpiry(tenantId, userId))
        assertEquals(4, service.listRecent(tenantId, userId, 10).size)
        assertEquals(emptyList(), service.listRecent(tenantId, UUID.randomUUID().toString(), 10))

        val revoked = service.revoke(
            MfaEnrollmentExemptionRevokeCommand(
                tenantId = tenantId,
                userId = userId,
                actorUserId = "admin-1",
                reason = "device returned",
            )
        )

        // Both active grants go, including the concurrently granted second one.
        assertEquals(2, revoked)
        assertNull(service.activeExemptionExpiry(tenantId, userId))
        val stored = service.listRecent(tenantId, userId, 10).filter { it.revokedBy == "admin-1" }
        assertEquals(2, stored.size)
        assertEquals(setOf("device returned"), stored.map { it.revokeReason }.toSet())
    }

    @Test
    fun v57RefusesGrantsTheDatabaseCouldNotExplainLater() {
        val tenantId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()

        // A window that ends before it starts is not a window.
        assertFailsWith<DataIntegrityViolationException> {
            dao.insert(exemption(tenantId, userId, expires = NOW.minusDays(1), granted = NOW))
        }
        // Revocation fields are all-or-nothing: a revoked row always says who and why.
        assertFailsWith<DataIntegrityViolationException> {
            dao.insert(
                exemption(tenantId, UUID.randomUUID().toString(), expires = NOW.plusDays(1)).apply {
                    status = "REVOKED"
                    revokedAt = NOW
                }
            )
        }
        // An active row cannot carry revocation evidence.
        assertFailsWith<DataIntegrityViolationException> {
            dao.insert(
                exemption(tenantId, UUID.randomUUID().toString(), expires = NOW.plusDays(1)).apply {
                    revokedBy = "admin-9"
                    revokeReason = "inconsistent"
                    revokedAt = NOW
                }
            )
        }
    }

    private fun exemption(
        tenant: String,
        user: String,
        expires: LocalDateTime,
        granted: LocalDateTime = NOW.minusHours(1),
    ) = AuthMfaEnrollmentExemption {
        id = UUID.randomUUID().toString()
        tenantId = tenant
        userId = user
        status = "ACTIVE"
        reason = "lost phone"
        grantedBy = "admin-1"
        grantedAt = granted
        expiresAt = expires
        revokedBy = null
        revokeReason = null
        revokedAt = null
    }

    private companion object {
        /**
         * Anchored to the wall clock, because the service is.
         *
         * This used to be a literal date, on the belief that the rows carrying their own windows kept the
         * test off the clock. Only [AuthMfaEnrollmentExemptionDao.findActive] takes the instant as an
         * argument; [IMfaEnrollmentExemptionService] reads its injected `Clock`, so it compared those windows
         * against the real today. The grant written at `NOW.plusDays(1)` therefore stopped being active a day
         * after the literal was written, and revocation started finding one row where the test expects two.
         *
         * Truncated to seconds so the stored timestamp round-trips exactly through H2.
         */
        val NOW: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
    }
}
