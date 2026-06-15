package io.kudos.ms.user.api.internal.controller.account

import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.core.account.api.UserAccountApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Test for [UserAccountInternalController].
 *
 * The controller forwards every call to the injected [UserAccountApi]. Two of its methods
 * ([UserAccountInternalController.getUserById], [UserAccountInternalController.getUsersByIds]) additionally
 * strip credential fields via `eraseCredentials()` at the service boundary; the rest are verbatim
 * pass-throughs. The api is mocked with Mockito so no Spring container / database is started.
 *
 * For each method the test asserts argument pass-through, returned value (including null / empty edges),
 * and — for the credential-erasing methods — that the four secret fields are nulled while every other
 * field survives unchanged and the original object is not mutated.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountInternalControllerTest {

    private val api = mock(UserAccountApi::class.java)
    private val controller = UserAccountInternalController(api)

    private fun fullEntry(id: String = "u-1"): UserAccountCacheEntry = UserAccountCacheEntry(
        id = id,
        username = "alice",
        tenantId = "tenant-1",
        loginPassword = "\$2a\$bcrypt-login-hash",
        securityPassword = "\$2a\$bcrypt-sec-hash",
        accountTypeDictCode = "NORMAL",
        accountStatusDictCode = "ACTIVE",
        defaultLocale = "zh_CN",
        defaultTimezone = "Asia/Shanghai",
        defaultCurrency = "CNY",
        lastLoginTime = LocalDateTime.of(2026, 1, 1, 8, 0),
        lastLoginIp = 167772161L,
        lastLogoutTime = LocalDateTime.of(2026, 1, 1, 18, 0),
        loginErrorTimes = 2,
        securityPasswordErrorTimes = 1,
        sessionKey = "session-secret",
        authenticationKey = "totp-secret",
        orgId = "org-1",
        supervisorId = "sup-1",
        remark = "备注",
        freezeType = "MANUAL",
        freezeTime = LocalDateTime.of(2026, 2, 1, 0, 0),
        freezeStartTime = LocalDateTime.of(2026, 2, 2, 0, 0),
        freezeEndTime = LocalDateTime.of(2026, 3, 1, 0, 0),
        freezeTitle = "冻结标题",
        freezeContent = "冻结内容",
        active = true,
        builtIn = false,
        createUserId = "creator",
        createUserName = "创建者",
        createTime = LocalDateTime.of(2025, 12, 1, 0, 0),
        updateUserId = "updater",
        updateUserName = "更新者",
        updateTime = LocalDateTime.of(2025, 12, 2, 0, 0),
    )

    /** Asserts all four credential fields are null but every other field equals the source. */
    private fun assertErased(source: UserAccountCacheEntry, erased: UserAccountCacheEntry) {
        assertNull(erased.loginPassword)
        assertNull(erased.securityPassword)
        assertNull(erased.authenticationKey)
        assertNull(erased.sessionKey)
        // non-credential fields untouched
        assertEquals(source.copy(
            loginPassword = null, securityPassword = null, authenticationKey = null, sessionKey = null,
        ), erased)
        // source object must not be mutated by the erasure
        assertEquals("\$2a\$bcrypt-login-hash", source.loginPassword)
        assertEquals("totp-secret", source.authenticationKey)
        assertEquals("session-secret", source.sessionKey)
    }

    @Test
    fun getUserById_erasesCredentials_andDelegates() {
        val source = fullEntry()
        whenCalled(api.getUserById("u-1")).thenReturn(source)

        val result = controller.getUserById("u-1")
        assertNotNull(result)
        assertNotSame(source, result, "erasure must return a defensive copy")
        assertErased(source, result)
        assertEquals("alice", result.username)
        assertEquals("u-1", result.id)
        verify(api).getUserById("u-1")
    }

    @Test
    fun getUserById_returnsNullOnMiss_andDelegates() {
        whenCalled(api.getUserById("missing")).thenReturn(null)

        assertNull(controller.getUserById("missing"))
        verify(api).getUserById("missing")
    }

    @Test
    fun getUsersByIds_erasesEveryValue_preservesKeysAndDelegates() {
        val a = fullEntry("a")
        val b = fullEntry("b").copy(username = "bob")
        val source = linkedMapOf("a" to a, "b" to b)
        whenCalled(api.getUsersByIds(listOf("a", "b"))).thenReturn(source)

        val result = controller.getUsersByIds(listOf("a", "b"))
        assertEquals(setOf("a", "b"), result.keys)
        assertErased(a, result.getValue("a"))
        assertErased(b, result.getValue("b"))
        assertEquals("bob", result.getValue("b").username)
        // original map values untouched
        assertEquals("\$2a\$bcrypt-login-hash", a.loginPassword)
        verify(api).getUsersByIds(listOf("a", "b"))
    }

    @Test
    fun getUsersByIds_emptyMap_andEmptyInput_delegates() {
        val ids = emptyList<String>()
        whenCalled(api.getUsersByIds(ids)).thenReturn(emptyMap())

        assertTrue(controller.getUsersByIds(ids).isEmpty())
        verify(api).getUsersByIds(ids)
    }

    @Test
    fun getUsersByIds_acceptsSetCollection_delegates() {
        val ids: Collection<String> = setOf("x")
        whenCalled(api.getUsersByIds(ids)).thenReturn(mapOf("x" to fullEntry("x")))

        val result = controller.getUsersByIds(ids)
        assertNull(result.getValue("x").loginPassword)
        verify(api).getUsersByIds(ids)
    }

    @Test
    fun getUserId_returnsId_andPreservesArgumentOrder() {
        whenCalled(api.getUserId("tenant-1", "alice")).thenReturn("u-9")
        // swapped args must reach the api swapped (tenantId/username not normalized)
        whenCalled(api.getUserId("alice", "tenant-1")).thenReturn(null)

        assertEquals("u-9", controller.getUserId("tenant-1", "alice"))
        assertNull(controller.getUserId("alice", "tenant-1"))
        verify(api).getUserId("tenant-1", "alice")
        verify(api).getUserId("alice", "tenant-1")
    }

    @Test
    fun getUserId_unicodeArguments_delegates() {
        whenCalled(api.getUserId("租户-甲", "爱丽丝")).thenReturn("用户-1")

        assertEquals("用户-1", controller.getUserId("租户-甲", "爱丽丝"))
        verify(api).getUserId("租户-甲", "爱丽丝")
    }

    @Test
    fun getUserOrgs_returnsList_andDelegates() {
        val org = mock(UserOrgCacheEntry::class.java)
        val expected = listOf(org)
        whenCalled(api.getUserOrgs("u-1")).thenReturn(expected)

        val actual = controller.getUserOrgs("u-1")
        assertSame(expected, actual)
        assertSame(org, actual[0])
        verify(api).getUserOrgs("u-1")
    }

    @Test
    fun getUserOrgs_emptyList_delegates() {
        whenCalled(api.getUserOrgs("u-empty")).thenReturn(emptyList())

        assertTrue(controller.getUserOrgs("u-empty").isEmpty())
        verify(api).getUserOrgs("u-empty")
    }

    @Test
    fun isUserInOrg_true_andFalse_withArgumentOrder() {
        whenCalled(api.isUserInOrg("u-1", "org-1")).thenReturn(true)
        whenCalled(api.isUserInOrg("org-1", "u-1")).thenReturn(false)

        assertTrue(controller.isUserInOrg("u-1", "org-1"))
        assertEquals(false, controller.isUserInOrg("org-1", "u-1"))
        verify(api).isUserInOrg("u-1", "org-1")
        verify(api).isUserInOrg("org-1", "u-1")
    }

    @Test
    fun getUserIds_returnsList_preservesOrder_andDelegates() {
        val expected = listOf("u3", "u1", "u2")
        whenCalled(api.getUserIds("tenant-1")).thenReturn(expected)

        val actual = controller.getUserIds("tenant-1")
        assertSame(expected, actual)
        assertEquals(listOf("u3", "u1", "u2"), actual)
        verify(api).getUserIds("tenant-1")
    }

    @Test
    fun getUserIds_emptyList_delegates() {
        whenCalled(api.getUserIds("tenant-empty")).thenReturn(emptyList())

        assertTrue(controller.getUserIds("tenant-empty").isEmpty())
        verify(api).getUserIds("tenant-empty")
    }

    @Test
    fun emptyStringArguments_arePassedThroughVerbatim() {
        whenCalled(api.getUserById("")).thenReturn(null)
        whenCalled(api.getUserId("", "")).thenReturn(null)
        whenCalled(api.isUserInOrg("", "")).thenReturn(false)

        assertNull(controller.getUserById(""))
        assertNull(controller.getUserId("", ""))
        assertEquals(false, controller.isUserInOrg("", ""))
        verify(api).getUserById("")
        verify(api).getUserId("", "")
        verify(api).isUserInOrg("", "")
    }
}
