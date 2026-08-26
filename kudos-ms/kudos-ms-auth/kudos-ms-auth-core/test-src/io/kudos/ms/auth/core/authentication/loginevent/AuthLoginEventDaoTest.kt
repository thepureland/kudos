package io.kudos.ms.auth.core.authentication.loginevent

import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.core.authentication.loginevent.dao.AuthLoginEventDao
import io.kudos.ms.auth.core.authentication.loginevent.model.AuthLoginEventObservation
import io.kudos.ms.auth.core.authentication.loginevent.model.AuthLoginEventRecordCommand
import io.kudos.ms.auth.core.authentication.loginevent.model.po.AuthLoginEvent
import io.kudos.ms.auth.core.authentication.loginevent.service.iservice.IAuthLoginEventService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Real Flyway/H2 regression for the V58 authentication outcome audit: what is stored, what the tenant-scoped
 * query returns, and the constraints that keep a row from claiming something it cannot support.
 */
@EnabledIfDockerInstalled
internal class AuthLoginEventDaoTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var service: IAuthLoginEventService

    @Resource
    private lateinit var dao: AuthLoginEventDao

    @Test
    fun v58RecordsOutcomesWithoutStoringTheAttemptedNameAndKeepsTenantsApart() {
        val tenantId = UUID.randomUUID().toString()
        val otherTenantId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()

        assertTrue(service.record(command(tenantId, userId = userId, identifier = "Alice")))
        assertTrue(
            service.record(
                command(tenantId, success = false, userId = null, identifier = "alice ", failureCode = "NO_SUCH_USER")
            )
        )
        assertTrue(service.record(command(otherTenantId, userId = UUID.randomUUID().toString())))

        val events = service.listRecent(tenantId, limit = 10)
        assertEquals(2, events.size)
        assertEquals(1, service.listRecent(otherTenantId, limit = 10).size)
        events.forEach { assertNotEquals("alice", it.identifierHash) }

        // Both attempts used the same name in different casing, so the audit can count them as one subject
        // even though the failure never matched an account.
        val byName = service.listRecent(tenantId, identifier = "ALICE", limit = 10)
        assertEquals(2, byName.size)
        assertEquals(setOf(true, false), byName.map { it.success }.toSet())
        assertEquals(1, service.listRecent(tenantId, successOnly = false, limit = 10).size)
        assertEquals(userId, service.listRecent(tenantId, userId = userId, limit = 10).single().userId)

        val success = byName.single { it.success }
        assertEquals(AuthenticationTransactionPurposeEnum.LOGIN, success.purpose)
        assertEquals(setOf("password", "totp"), success.amr)
        assertEquals(3232235777L, success.loginIp)
        assertNull(success.failureCode)
    }

    @Test
    fun v58AllowsOnlyOneRowPerTransactionAndRefusesContradictoryEvidence() {
        val tenantId = UUID.randomUUID().toString()
        val transactionId = UUID.randomUUID().toString()
        val command = command(tenantId, userId = UUID.randomUUID().toString()).copy(transactionId = transactionId)

        assertTrue(service.record(command))
        // A transaction turns terminal once, so a second feed is a replay and is suppressed rather than doubled.
        assertFalse(service.record(command))
        assertEquals(1, service.listRecent(tenantId, limit = 10).size)

        // The database refuses the same thing the service does, so a direct writer cannot get around it.
        assertFailsWith<DataIntegrityViolationException> {
            dao.insert(po(tenantId).apply { this.transactionId = transactionId })
        }
        assertFailsWith<DataIntegrityViolationException> {
            dao.insert(po(tenantId).apply { success = true; failureCode = "SHOULD_NOT_HAVE_ONE" })
        }
        assertFailsWith<DataIntegrityViolationException> {
            dao.insert(po(tenantId).apply { success = true; userId = null })
        }
        assertFailsWith<DataIntegrityViolationException> {
            dao.insert(po(tenantId).apply { success = false; failureCode = null })
        }
    }

    private fun command(
        tenantId: String,
        success: Boolean = true,
        userId: String? = UUID.randomUUID().toString(),
        identifier: String? = "alice",
        failureCode: String? = null,
    ) = AuthLoginEventRecordCommand(
        transactionId = UUID.randomUUID().toString(),
        purpose = AuthenticationTransactionPurposeEnum.LOGIN,
        success = success,
        tenantId = tenantId,
        userId = userId,
        identifier = identifier,
        authenticationMethod = "password",
        failureCode = failureCode ?: if (success) null else "INVALID_CREDENTIALS",
        acr = "urn:kudos:acr:mfa",
        amr = setOf("password", "totp"),
        observation = AuthLoginEventObservation(
            loginIp = 3232235777L,
            loginBrowser = "Firefox",
            loginOs = "Linux",
            userAgent = "Mozilla/5.0",
        ),
        occurredAt = NOW,
    )

    private fun po(tenant: String) = AuthLoginEvent {
        id = UUID.randomUUID().toString()
        tenantId = tenant
        userId = UUID.randomUUID().toString()
        identifierHash = null
        providerId = null
        authenticationMethod = "password"
        transactionId = UUID.randomUUID().toString()
        purpose = "LOGIN"
        sessionId = null
        success = true
        failureCode = null
        acr = null
        amr = null
        loginIp = null
        loginLocation = null
        loginDevice = null
        loginBrowser = null
        loginOs = null
        userAgent = null
        occurredAt = NOW
    }

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 8, 25, 10, 0)
    }
}
