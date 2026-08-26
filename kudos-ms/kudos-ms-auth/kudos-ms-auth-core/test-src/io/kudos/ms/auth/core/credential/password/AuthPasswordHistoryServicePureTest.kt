package io.kudos.ms.auth.core.credential.password

import io.kudos.base.security.PasswordKit
import io.kudos.ability.security.common.init.SecurityCommonAutoConfiguration
import io.kudos.ms.auth.core.credential.password.dao.AuthPasswordHistoryDao
import io.kudos.ms.auth.core.credential.password.init.PasswordHistoryProperties
import io.kudos.ms.auth.core.credential.password.model.po.AuthPasswordHistory
import io.kudos.ms.auth.core.credential.password.service.AuthPasswordHistoryService
import io.kudos.ms.user.core.account.security.PasswordPolicyContext
import io.kudos.ms.user.core.account.security.PasswordPurpose
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class AuthPasswordHistoryServicePureTest {

    private val dao = mock(AuthPasswordHistoryDao::class.java)
    private val properties = PasswordHistoryProperties()
    private val passwordEncoder = SecurityCommonAutoConfiguration().passwordEncoder()
    private val service = AuthPasswordHistoryService(dao, properties, passwordEncoder)
    private val context = PasswordPolicyContext(
        purpose = PasswordPurpose.LOGIN,
        userId = "u1",
        username = "alice",
        tenantId = "t1",
    )

    @Test
    fun isReused_matchesOnlyRetainedHashes() {
        val oldHash = PasswordKit.hash("previously used strong password", 4)
        whenCalled(dao.findRecent("t1", "u1", "LOGIN", 5)).thenReturn(
            listOf(history(oldHash))
        )

        assertTrue(service.isReused("previously used strong password", context))
        assertFalse(service.isReused("a genuinely different password", context))
    }

    @Test
    fun record_persistsOnlyHashAndTrimsToConfiguredSize() {
        properties.historySize = 3
        val hash = PasswordKit.hash("retired strong password", 4)

        service.record(hash, context)

        val captor = org.mockito.ArgumentCaptor.forClass(AuthPasswordHistory::class.java)
        verify(dao).insert(captureHistory(captor))
        assertTrue(captor.value.passwordHash == hash)
        assertTrue(captor.value.passwordHash != "retired strong password")
        verify(dao).trimToSize("t1", "u1", "LOGIN", 3)
    }

    @Test
    fun versionedBcryptIsAcceptedAndMatched() {
        val hash = requireNotNull(passwordEncoder.encode("previously used strong password"))
        whenCalled(dao.findRecent("t1", "u1", "LOGIN", 5)).thenReturn(listOf(history(hash)))

        assertTrue(service.isReused("previously used strong password", context))
        service.record(hash, context)

        verify(dao).insert(anyHistory())
    }

    @Test
    fun disabledHistory_neitherReadsNorWrites() {
        properties.enabled = false

        assertFalse(service.isReused("any candidate password", context))
        service.record(PasswordKit.hash("retired strong password", 4), context)

        verify(dao, never()).findRecent("t1", "u1", "LOGIN", 5)
        verify(dao, never()).insert(anyHistory())
    }

    @Test
    fun record_rejectsRawOrUnsupportedValues() {
        assertFailsWith<IllegalArgumentException> {
            service.record("raw password must never be stored", context)
        }
        verify(dao, never()).insert(anyHistory())
    }

    private fun history(hash: String): AuthPasswordHistory = AuthPasswordHistory {
        id = "h1"
        tenantId = "t1"
        userId = "u1"
        purpose = "LOGIN"
        passwordHash = hash
        recordedAt = LocalDateTime.now()
    }

    private fun anyHistory(): AuthPasswordHistory =
        ArgumentMatchers.any(AuthPasswordHistory::class.java) ?: history(PasswordKit.hash("fallback password value", 4))

    private fun captureHistory(captor: org.mockito.ArgumentCaptor<AuthPasswordHistory>): AuthPasswordHistory =
        captor.capture() ?: history(PasswordKit.hash("fallback password value", 4))
}
