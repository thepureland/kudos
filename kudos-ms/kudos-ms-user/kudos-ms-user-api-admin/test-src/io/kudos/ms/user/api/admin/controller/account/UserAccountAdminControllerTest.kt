package io.kudos.ms.user.api.admin.controller.account

import io.kudos.base.query.PagingSearchResult
import io.kudos.ms.user.common.account.vo.response.AuthKeySetup
import io.kudos.ms.user.common.account.vo.response.UserAccountDetail
import io.kudos.ms.user.common.account.vo.response.UserAccountEdit
import io.kudos.ms.user.common.account.vo.response.UserAccountRow
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Test for [UserAccountAdminController].
 *
 * The credential-erasing read overrides (pagingSearch / getDetail / getEdit) and every thin write
 * delegation are exercised with the service mocked via Mockito; no Spring container or DB is started.
 * The base-controller `service` field is injected via reflection.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountAdminControllerTest {

    private val service = mock(IUserAccountService::class.java)
    private val controller = UserAccountAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    // ---------- read overrides: credential erasure ----------

    @Test
    fun pagingSearch_erasesCredentialsOfEveryRowAndPreservesTotalAndOrder() {
        val row1 = UserAccountRow(
            id = "u1", username = "alice", sessionKey = "sk1", authenticationKey = "ak1",
            orgId = "o1", active = true,
        )
        val row2 = UserAccountRow(
            id = "u2", username = "bob", sessionKey = "sk2", authenticationKey = "ak2",
            orgId = "o2", active = false,
        )
        val query = mockQuery()
        whenCalled(service.pagingSearch(query)).thenReturn(PagingSearchResult(listOf(row1, row2), 42))

        val result = controller.pagingSearch(query)

        assertEquals(42, result.totalCount)
        assertEquals(2, result.data.size)
        // order preserved
        assertEquals(listOf("u1", "u2"), result.data.map { it.id })
        // every row's credentials erased, other fields kept
        result.data.forEach {
            assertNull(it.sessionKey)
            assertNull(it.authenticationKey)
        }
        assertEquals("alice", result.data[0].username)
        assertEquals(true, result.data[0].active)
        assertEquals("bob", result.data[1].username)
        // original instances untouched (defensive copy)
        assertEquals("ak1", row1.authenticationKey)
        assertEquals("sk2", row2.sessionKey)
        verify(service).pagingSearch(query)
    }

    @Test
    fun pagingSearch_emptyPageKeepsTotalCount() {
        val query = mockQuery()
        whenCalled(service.pagingSearch(query)).thenReturn(PagingSearchResult(emptyList<UserAccountRow>(), 0))

        val result = controller.pagingSearch(query)

        assertTrue(result.data.isEmpty())
        assertEquals(0, result.totalCount)
    }

    @Test
    fun getDetail_erasesCredentials() {
        val detail = UserAccountDetail(
            id = "u1", username = "alice", tenantId = "t1",
            loginPassword = "p", securityPassword = "s", sessionKey = "sk", authenticationKey = "ak",
            orgId = "o1",
        )
        whenCalled(service.get("u1", UserAccountDetail::class)).thenReturn(detail)

        val result = controller.getDetail("u1")

        assertNull(result.loginPassword)
        assertNull(result.securityPassword)
        assertNull(result.sessionKey)
        assertNull(result.authenticationKey)
        assertEquals("alice", result.username)
        assertEquals("o1", result.orgId)
        // original untouched
        assertEquals("p", detail.loginPassword)
    }

    @Test
    fun getEdit_erasesCredentials() {
        val edit = UserAccountEdit(
            id = "u1", username = "alice",
            loginPassword = "p", securityPassword = "s", sessionKey = "sk", authenticationKey = "ak",
            remark = "keep-me",
        )
        whenCalled(service.get("u1", UserAccountEdit::class)).thenReturn(edit)

        val result = controller.getEdit("u1")

        assertNull(result.loginPassword)
        assertNull(result.securityPassword)
        assertNull(result.sessionKey)
        assertNull(result.authenticationKey)
        assertEquals("keep-me", result.remark)
        assertEquals("p", edit.loginPassword)
    }

    // ---------- write delegations ----------

    @Test
    fun updateActive_delegatesBothBooleans() {
        whenCalled(service.updateActive("u1", true)).thenReturn(true)
        whenCalled(service.updateActive("u2", false)).thenReturn(false)
        assertTrue(controller.updateActive("u1", true))
        assertFalse(controller.updateActive("u2", false))
        verify(service).updateActive("u1", true)
        verify(service).updateActive("u2", false)
    }

    @Test
    fun resetPassword_delegates() {
        whenCalled(service.resetPassword("u1", "newPwd")).thenReturn(true)
        assertTrue(controller.resetPassword("u1", "newPwd"))
        verify(service).resetPassword("u1", "newPwd")
    }

    @Test
    fun resetPassword_falseOnFailure() {
        whenCalled(service.resetPassword("u1", "x")).thenReturn(false)
        assertFalse(controller.resetPassword("u1", "x"))
    }

    @Test
    fun resetSecurityPassword_delegates() {
        whenCalled(service.resetSecurityPassword("u1", "sec")).thenReturn(true)
        assertTrue(controller.resetSecurityPassword("u1", "sec"))
        verify(service).resetSecurityPassword("u1", "sec")
    }

    @Test
    fun resetAuthKey_delegatesAndForwardsExplicitIssuer() {
        val setup = AuthKeySetup(secret = "SECRET", otpauthUrl = "otpauth://totp/x")
        whenCalled(service.resetAuthKey("u1", "alice", "myIssuer")).thenReturn(setup)
        assertSame(setup, controller.resetAuthKey("u1", "alice", "myIssuer"))
        verify(service).resetAuthKey("u1", "alice", "myIssuer")
    }

    @Test
    fun resetAuthKey_forwardsKudosIssuer() {
        // The Spring "kudos" default is supplied by request binding; from Kotlin it must be passed explicitly.
        val setup = AuthKeySetup(secret = "S", otpauthUrl = "u")
        whenCalled(service.resetAuthKey("u1", "alice", "kudos")).thenReturn(setup)
        assertSame(setup, controller.resetAuthKey("u1", "alice", "kudos"))
        verify(service).resetAuthKey("u1", "alice", "kudos")
    }

    @Test
    fun resetAuthKey_nullWhenUserMissing() {
        whenCalled(service.resetAuthKey("missing", "x", "kudos")).thenReturn(null)
        assertNull(controller.resetAuthKey("missing", "x", "kudos"))
    }

    @Test
    fun cleanAuthKey_delegatesBothOutcomes() {
        whenCalled(service.cleanAuthKey("u1")).thenReturn(true)
        whenCalled(service.cleanAuthKey("u2")).thenReturn(false)
        assertTrue(controller.cleanAuthKey("u1"))
        assertFalse(controller.cleanAuthKey("u2"))
    }

    @Test
    fun verifyAuthCode_delegatesAndForwardsCode() {
        whenCalled(service.verifyAuthCode("u1", 123456L)).thenReturn(true)
        whenCalled(service.verifyAuthCode("u1", 1L)).thenReturn(false)
        assertTrue(controller.verifyAuthCode("u1", 123456L))
        assertFalse(controller.verifyAuthCode("u1", 1L))
        verify(service).verifyAuthCode("u1", 123456L)
        verify(service).verifyAuthCode("u1", 1L)
    }

    @Test
    fun freezeAccount_forwardsAllSixArgs() {
        val start = LocalDateTime.of(2026, 1, 1, 0, 0)
        val end = LocalDateTime.of(2026, 2, 1, 0, 0)
        whenCalled(service.freezeAccount("u1", "manual", "t", "c", start, end)).thenReturn(true)
        assertTrue(controller.freezeAccount("u1", "manual", "t", "c", start, end))
        verify(service).freezeAccount("u1", "manual", "t", "c", start, end)
    }

    @Test
    fun freezeAccount_nullableArgsForwardedAsNull() {
        whenCalled(service.freezeAccount("u1", "auto", null, null, null, null)).thenReturn(true)
        assertTrue(controller.freezeAccount("u1", "auto", null, null, null, null))
        verify(service).freezeAccount("u1", "auto", null, null, null, null)
    }

    @Test
    fun unfreezeAccount_delegatesBothOutcomes() {
        whenCalled(service.unfreezeAccount("u1")).thenReturn(true)
        whenCalled(service.unfreezeAccount("u2")).thenReturn(false)
        assertTrue(controller.unfreezeAccount("u1"))
        assertFalse(controller.unfreezeAccount("u2"))
    }

    @Test
    fun cleanExpiredFreezes_returnsCleanedCount() {
        whenCalled(service.cleanExpiredFreezes()).thenReturn(7)
        assertEquals(7, controller.cleanExpiredFreezes())
        verify(service).cleanExpiredFreezes()
    }

    @Test
    fun cleanExpiredFreezes_zeroWhenNothingToClean() {
        whenCalled(service.cleanExpiredFreezes()).thenReturn(0)
        assertEquals(0, controller.cleanExpiredFreezes())
    }

    private fun mockQuery() =
        io.kudos.ms.user.common.account.vo.request.UserAccountQuery()

}
