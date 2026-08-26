package io.kudos.ms.auth.core.authentication.loginevent

import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.core.authentication.loginevent.dao.AuthLoginEventDao
import io.kudos.ms.auth.core.authentication.loginevent.model.AuthLoginEventObservation
import io.kudos.ms.auth.core.authentication.loginevent.model.AuthLoginEventRecordCommand
import io.kudos.ms.auth.core.authentication.loginevent.model.po.AuthLoginEvent
import io.kudos.ms.auth.core.authentication.loginevent.service.impl.AuthLoginEventService
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class AuthLoginEventServiceTest {
    private val dao = mock(AuthLoginEventDao::class.java)
    private val service = AuthLoginEventService(dao)

    @Test
    fun theAttemptedNameIsStoredOnlyAsADigest() {
        service.record(command(identifier = "Alice@Example.com "))

        val stored = ArgumentCaptor.forClass(AuthLoginEvent::class.java)
        verify(dao).insert(stored.capture() ?: po())
        val hash = stored.value.identifierHash
        assertNotEquals("alice@example.com", hash)
        assertEquals(64, hash?.length)
        // Case and surrounding whitespace do not produce a different subject, so counting attempts against one
        // name actually works.
        assertEquals(service.identifierHash("alice@example.com"), hash)
        assertEquals(service.identifierHash(" ALICE@EXAMPLE.COM"), hash)
    }

    @Test
    fun aSearchTermIsHashedTheSameWayBeforeItReachesTheQuery() {
        `when`(dao.findRecent("t-1", null, service.identifierHash("alice"), null, 50)).thenReturn(emptyList())

        assertEquals(emptyList(), service.listRecent("t-1", identifier = " Alice ", limit = 50))

        verify(dao).findRecent("t-1", null, service.identifierHash("alice"), null, 50)
    }

    @Test
    fun evidenceThatContradictsTheOutcomeIsRefused() {
        // Nothing is written when the row would claim a success nobody can be attributed to, or a failure with
        // no reason — the table's own CHECK says the same thing, this just fails earlier and more clearly.
        assertFalse(service.record(command(success = true, userId = null)))
        // Built directly: the helper fills in a failure code, which is exactly what this case must not have.
        assertFalse(service.record(command(success = false).copy(failureCode = null)))

        verify(dao, never()).insert(any(AuthLoginEvent::class.java) ?: po())
    }

    @Test
    fun aRepeatedFeedForTheSameTransactionIsSuppressedRatherThanDuplicated() {
        `when`(dao.findByTransaction("tx-1")).thenReturn(po())

        assertFalse(service.record(command()))

        verify(dao, never()).insert(any(AuthLoginEvent::class.java) ?: po())
    }

    @Test
    fun anAuditStoreOutageNeverPropagatesIntoTheAuthenticationResult() {
        `when`(dao.findByTransaction("tx-1")).thenThrow(IllegalStateException("audit database is down"))

        // The decision was already made and persisted; letting this throw would deny service over bookkeeping.
        assertFalse(service.record(command()))
    }

    @Test
    fun anOverlongUserAgentIsTruncatedRatherThanCostingTheWholeRow() {
        service.record(command(observation = AuthLoginEventObservation(userAgent = "u".repeat(900))))

        val stored = ArgumentCaptor.forClass(AuthLoginEvent::class.java)
        verify(dao).insert(stored.capture() ?: po())
        assertEquals(512, stored.value.userAgent?.length)
    }

    @Test
    fun aSuccessCarriesItsSubjectMethodAndAuthenticationContext() {
        assertTrue(
            service.record(
                command(
                    acr = "urn:kudos:acr:mfa",
                    amr = setOf("password", "totp"),
                    sessionId = "session-1",
                    providerId = "google",
                )
            )
        )

        val stored = ArgumentCaptor.forClass(AuthLoginEvent::class.java)
        verify(dao).insert(stored.capture() ?: po())
        assertEquals("urn:kudos:acr:mfa", stored.value.acr)
        assertEquals("password,totp", stored.value.amr)
        assertEquals("session-1", stored.value.sessionId)
        assertEquals("google", stored.value.providerId)
        assertNull(stored.value.failureCode)
    }

    private fun command(
        success: Boolean = true,
        userId: String? = "u-1",
        identifier: String? = "alice",
        failureCode: String? = null,
        acr: String? = null,
        amr: Set<String> = emptySet(),
        sessionId: String? = null,
        providerId: String? = null,
        observation: AuthLoginEventObservation = AuthLoginEventObservation.EMPTY,
    ) = AuthLoginEventRecordCommand(
        transactionId = "tx-1",
        purpose = AuthenticationTransactionPurposeEnum.LOGIN,
        success = success,
        tenantId = "t-1",
        userId = userId,
        identifier = identifier,
        providerId = providerId,
        authenticationMethod = "password",
        sessionId = sessionId,
        failureCode = failureCode ?: if (success) null else "INVALID_CREDENTIALS",
        acr = acr,
        amr = amr,
        observation = observation,
        occurredAt = NOW,
    )

    private fun po() = AuthLoginEvent {
        id = "event-1"
        tenantId = "t-1"
        userId = "u-1"
        identifierHash = null
        providerId = null
        authenticationMethod = "password"
        transactionId = "tx-1"
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
        val NOW: LocalDateTime = LocalDateTime.parse("2026-08-25T10:00:00")
    }
}
