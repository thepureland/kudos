package io.kudos.ms.auth.core.authentication.credentialrevocation

import io.kudos.ms.auth.core.authentication.credentialrevocation.dao.AuthCredentialRevocationDao
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.po.AuthCredentialRevocation
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Real Flyway/H2 regression for the V59 revocation record: tenant-scoped history and the constraints that keep
 * a row from describing a revocation that could not have happened.
 */
@EnabledIfDockerInstalled
internal class AuthCredentialRevocationDaoTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var dao: AuthCredentialRevocationDao

    @Test
    fun v59KeepsAnAppendOnlyHistoryScopedToTheTenant() {
        val tenantId = UUID.randomUUID().toString()
        val otherTenantId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()

        dao.insert(revocation(tenantId, userId, revokedAt = NOW.minusHours(2)))
        dao.insert(totpRevocation(tenantId, userId, revokedAt = NOW.minusHours(1)))
        dao.insert(revocation(otherTenantId, UUID.randomUUID().toString()))

        val history = dao.findRecent(tenantId, userId, 10)
        assertEquals(2, history.size)
        // Newest first: an operator reading the history is looking for what just happened.
        assertEquals(listOf("TOTP", "WEBAUTHN"), history.map { it.credentialType })
        assertEquals(1, dao.findRecent(otherTenantId, null, 10).size)
        assertEquals(emptyList(), dao.findRecent(tenantId, UUID.randomUUID().toString(), 10))
    }

    @Test
    fun v59RefusesRowsThatCouldNotDescribeARealRevocation() {
        val tenantId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()

        // A WebAuthn revocation names exactly one credential...
        assertFailsWith<DataIntegrityViolationException> {
            dao.insert(revocation(tenantId, userId).apply { credentialRef = null })
        }
        assertFailsWith<DataIntegrityViolationException> {
            dao.insert(revocation(tenantId, userId).apply { credentialFingerprint = null })
        }
        // ...while a TOTP authenticator is singular per account and has none to name.
        assertFailsWith<DataIntegrityViolationException> {
            dao.insert(totpRevocation(tenantId, userId).apply { credentialRef = "row-1" })
        }
        // A revocation without a stated reason is not an audit record.
        assertFailsWith<DataIntegrityViolationException> {
            dao.insert(revocation(tenantId, userId).apply { reason = "" })
        }
        assertFailsWith<DataIntegrityViolationException> {
            dao.insert(revocation(tenantId, userId).apply { credentialType = "SMART_CARD" })
        }
    }

    private fun revocation(
        tenant: String,
        user: String,
        revokedAt: LocalDateTime = NOW,
    ) = AuthCredentialRevocation {
        id = UUID.randomUUID().toString()
        tenantId = tenant
        userId = user
        credentialType = "WEBAUTHN"
        credentialRef = UUID.randomUUID().toString()
        credentialFingerprint = "A".repeat(43)
        actorUserId = "admin-1"
        reason = "authenticator reported stolen"
        securityEventId = null
        leftWithoutFactor = false
        this.revokedAt = revokedAt
    }

    private fun totpRevocation(
        tenant: String,
        user: String,
        revokedAt: LocalDateTime = NOW,
    ) = revocation(tenant, user, revokedAt).apply {
        credentialType = "TOTP"
        credentialRef = null
        credentialFingerprint = null
        leftWithoutFactor = true
    }

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 8, 26, 10, 0)
    }
}
