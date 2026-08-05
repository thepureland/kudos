package io.kudos.ms.user.api.internal.controller.org

import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.core.org.api.UserOrgApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Test for [UserOrgInternalController].
 *
 * The controller forwards every call to the injected [UserOrgApi]. Two of its methods
 * ([UserOrgInternalController.getOrgAdmins], [UserOrgInternalController.getOrgUsers]) map each returned
 * account through `eraseCredentials()` at the service boundary; the rest are verbatim pass-throughs.
 * The api is mocked with Mockito so no Spring container / database is started.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserOrgInternalControllerTest {

    private val api = mock(UserOrgApi::class.java)
    private val controller = UserOrgInternalController(api)

    private fun account(id: String): UserAccountCacheEntry = UserAccountCacheEntry(
        id = id,
        username = "user-$id",
        tenantId = "tenant-1",
        loginPassword = "login-hash-$id",
        securityPassword = "sec-hash-$id",
        accountTypeDictCode = null,
        accountStatusDictCode = null,
        defaultLocale = null,
        defaultTimezone = null,
        defaultCurrency = null,
        lastLoginTime = null,
        lastLoginIp = null,
        lastLogoutTime = null,
        loginErrorTimes = null,
        securityPasswordErrorTimes = null,
        sessionKey = "session-$id",
        authenticationKey = "totp-$id",
        orgId = "org-1",
        supervisorId = null,
        remark = null,
        active = true,
        builtIn = false,
        createUserId = null,
        createUserName = null,
        createTime = LocalDateTime.of(2026, 1, 1, 0, 0),
        updateUserId = null,
        updateUserName = null,
        updateTime = null,
    )

    private fun assertErased(source: UserAccountCacheEntry, erased: UserAccountCacheEntry) {
        assertNull(erased.loginPassword)
        assertNull(erased.securityPassword)
        assertNull(erased.authenticationKey)
        assertNull(erased.sessionKey)
        assertEquals(source.id, erased.id)
        assertEquals(source.username, erased.username)
        assertEquals(source.orgId, erased.orgId)
        // source not mutated
        assertEquals("login-hash-${source.id}", source.loginPassword)
        assertEquals("totp-${source.id}", source.authenticationKey)
    }

    @Test
    fun getOrgById_returnsEntryVerbatim_andDelegates() {
        val entry = mock(UserOrgCacheEntry::class.java)
        whenCalled(api.getOrgById("org-1")).thenReturn(entry)

        assertSame(entry, controller.getOrgById("org-1"))
        verify(api).getOrgById("org-1")
    }

    @Test
    fun getOrgById_returnsNullOnMiss_andDelegates() {
        whenCalled(api.getOrgById("missing")).thenReturn(null)

        assertNull(controller.getOrgById("missing"))
        verify(api).getOrgById("missing")
    }

    @Test
    fun getOrgsByIds_returnsMapVerbatim_andDelegates() {
        val a = mock(UserOrgCacheEntry::class.java)
        val b = mock(UserOrgCacheEntry::class.java)
        val expected = mapOf("a" to a, "b" to b)
        whenCalled(api.getOrgsByIds(listOf("a", "b"))).thenReturn(expected)

        val actual = controller.getOrgsByIds(listOf("a", "b"))
        assertSame(expected, actual)
        assertSame(a, actual["a"])
        assertSame(b, actual["b"])
        verify(api).getOrgsByIds(listOf("a", "b"))
    }

    @Test
    fun getOrgsByIds_emptyInputAndResult_acceptsSet_delegates() {
        val ids: Collection<String> = setOf<String>()
        whenCalled(api.getOrgsByIds(ids)).thenReturn(emptyMap())

        assertTrue(controller.getOrgsByIds(ids).isEmpty())
        verify(api).getOrgsByIds(ids)
    }

    @Test
    fun getOrgIds_returnsList_preservesOrder_andDelegates() {
        val expected = listOf("o3", "o1", "o2")
        whenCalled(api.getOrgIds("tenant-1")).thenReturn(expected)

        val actual = controller.getOrgIds("tenant-1")
        assertSame(expected, actual)
        assertEquals(listOf("o3", "o1", "o2"), actual)
        verify(api).getOrgIds("tenant-1")
    }

    @Test
    fun getOrgIds_emptyList_delegates() {
        whenCalled(api.getOrgIds("tenant-empty")).thenReturn(emptyList())

        assertTrue(controller.getOrgIds("tenant-empty").isEmpty())
        verify(api).getOrgIds("tenant-empty")
    }

    @Test
    fun getOrgAdmins_erasesCredentials_preservesOrder_andDelegates() {
        val a1 = account("a1")
        val a2 = account("a2")
        whenCalled(api.getOrgAdmins("org-1")).thenReturn(listOf(a1, a2))

        val actual = controller.getOrgAdmins("org-1")
        assertEquals(2, actual.size)
        assertEquals(listOf("a1", "a2"), actual.map { it.id })
        assertErased(a1, actual[0])
        assertErased(a2, actual[1])
        verify(api).getOrgAdmins("org-1")
    }

    @Test
    fun getOrgAdmins_emptyList_delegates() {
        whenCalled(api.getOrgAdmins("org-empty")).thenReturn(emptyList())

        assertTrue(controller.getOrgAdmins("org-empty").isEmpty())
        verify(api).getOrgAdmins("org-empty")
    }

    @Test
    fun getOrgUsers_erasesCredentials_andDelegates() {
        val u1 = account("u1")
        whenCalled(api.getOrgUsers("org-1")).thenReturn(listOf(u1))

        val actual = controller.getOrgUsers("org-1")
        assertEquals(1, actual.size)
        assertErased(u1, actual[0])
        verify(api).getOrgUsers("org-1")
    }

    @Test
    fun getOrgUsers_emptyList_delegates() {
        whenCalled(api.getOrgUsers("org-empty")).thenReturn(emptyList())

        assertTrue(controller.getOrgUsers("org-empty").isEmpty())
        verify(api).getOrgUsers("org-empty")
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
    fun getChildOrgs_returnsListVerbatim_andDelegates() {
        val child = mock(UserOrgCacheEntry::class.java)
        val expected = listOf(child)
        whenCalled(api.getChildOrgs("org-1")).thenReturn(expected)

        val actual = controller.getChildOrgs("org-1")
        assertSame(expected, actual)
        assertSame(child, actual[0])
        verify(api).getChildOrgs("org-1")
    }

    @Test
    fun getChildOrgs_emptyList_delegates() {
        whenCalled(api.getChildOrgs("org-leaf")).thenReturn(emptyList())

        assertTrue(controller.getChildOrgs("org-leaf").isEmpty())
        verify(api).getChildOrgs("org-leaf")
    }

    @Test
    fun getParentOrg_returnsEntryVerbatim_andDelegates() {
        val parent = mock(UserOrgCacheEntry::class.java)
        whenCalled(api.getParentOrg("org-1")).thenReturn(parent)

        assertSame(parent, controller.getParentOrg("org-1"))
        verify(api).getParentOrg("org-1")
    }

    @Test
    fun getParentOrg_returnsNullForRoot_andDelegates() {
        whenCalled(api.getParentOrg("org-root")).thenReturn(null)

        assertNull(controller.getParentOrg("org-root"))
        verify(api).getParentOrg("org-root")
    }

    @Test
    fun emptyAndUnicodeStringArguments_arePassedThroughVerbatim() {
        whenCalled(api.getOrgById("")).thenReturn(null)
        whenCalled(api.getOrgIds("租户-甲")).thenReturn(listOf("机构-1"))

        assertNull(controller.getOrgById(""))
        assertEquals(listOf("机构-1"), controller.getOrgIds("租户-甲"))
        verify(api).getOrgById("")
        verify(api).getOrgIds("租户-甲")
    }
}
