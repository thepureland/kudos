package io.kudos.ms.user.api.admin.controller.org

import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.common.org.vo.response.UserOrgTreeRow
import io.kudos.ms.user.core.org.service.iservice.IUserOrgService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Test for [UserOrgAdminController].
 *
 * Thin write delegations plus the two credential-erasing read endpoints (getOrgUsers / getOrgAdmins)
 * are exercised with the service mocked via Mockito; no Spring container or DB is started.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserOrgAdminControllerTest {

    private val service = mock(IUserOrgService::class.java)
    private val controller = UserOrgAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    private fun cacheEntry(id: String, username: String) = UserAccountCacheEntry(
        id = id,
        username = username,
        tenantId = "t-1",
        loginPassword = "bcrypt-login",
        securityPassword = "bcrypt-sec",
        accountTypeDictCode = "1",
        accountStatusDictCode = "0",
        defaultLocale = "zh_CN",
        defaultTimezone = "Asia/Shanghai",
        defaultCurrency = "CNY",
        lastLoginTime = null,
        lastLoginIp = 1L,
        lastLogoutTime = null,
        loginErrorTimes = 0,
        securityPasswordErrorTimes = 0,
        sessionKey = "sess-secret",
        authenticationKey = "totp-secret",
        orgId = "o-1",
        supervisorId = null,
        remark = null,
        active = true,
        builtIn = false,
        createUserId = null,
        createUserName = null,
        createTime = null,
        updateUserId = null,
        updateUserName = null,
        updateTime = null,
    )

    @Test
    fun updateActive_delegatesBothBooleans() {
        whenCalled(service.updateActive("o1", true)).thenReturn(true)
        whenCalled(service.updateActive("o2", false)).thenReturn(false)
        assertTrue(controller.updateActive("o1", true))
        assertFalse(controller.updateActive("o2", false))
        verify(service).updateActive("o1", true)
        verify(service).updateActive("o2", false)
    }

    @Test
    fun moveOrg_forwardsAllThreeArgs() {
        whenCalled(service.moveOrg("o1", "p1", 5)).thenReturn(true)
        assertTrue(controller.moveOrg("o1", "p1", 5))
        verify(service).moveOrg("o1", "p1", 5)
    }

    @Test
    fun moveOrg_nullableArgsForwardedAsNull() {
        whenCalled(service.moveOrg("o1", null, null)).thenReturn(false)
        assertFalse(controller.moveOrg("o1", null, null))
        verify(service).moveOrg("o1", null, null)
    }

    @Test
    fun getOrgTree_forwardsTenantAndParentAndPreservesOrder() {
        val nodes = listOf(UserOrgTreeRow(id = "n1"), UserOrgTreeRow(id = "n2"))
        whenCalled(service.getOrgTree("t1", "root")).thenReturn(nodes)
        val result = controller.getOrgTree("t1", "root")
        assertSame(nodes, result)
        assertEquals(listOf("n1", "n2"), result.map { it.id })
        verify(service).getOrgTree("t1", "root")
    }

    @Test
    fun getOrgTree_nullParentForwarded() {
        whenCalled(service.getOrgTree("t1", null)).thenReturn(emptyList())
        assertTrue(controller.getOrgTree("t1", null).isEmpty())
        verify(service).getOrgTree("t1", null)
    }

    @Test
    fun getOrgUsers_erasesCredentialsOfEveryEntryAndPreservesOrder() {
        val e1 = cacheEntry("u1", "alice")
        val e2 = cacheEntry("u2", "bob")
        whenCalled(service.getOrgUsers("o1")).thenReturn(listOf(e1, e2))

        val result = controller.getOrgUsers("o1")

        assertEquals(listOf("u1", "u2"), result.map { it.id })
        result.forEach {
            assertNull(it.loginPassword)
            assertNull(it.securityPassword)
            assertNull(it.sessionKey)
            assertNull(it.authenticationKey)
        }
        assertEquals("alice", result[0].username)
        assertEquals("bob", result[1].username)
        // originals untouched
        assertEquals("bcrypt-login", e1.loginPassword)
        assertEquals("totp-secret", e2.authenticationKey)
        verify(service).getOrgUsers("o1")
    }

    @Test
    fun getOrgUsers_emptyResult() {
        whenCalled(service.getOrgUsers("o1")).thenReturn(emptyList())
        assertTrue(controller.getOrgUsers("o1").isEmpty())
    }

    @Test
    fun getOrgAdmins_erasesCredentials() {
        val e1 = cacheEntry("a1", "admin")
        whenCalled(service.getOrgAdmins("o1")).thenReturn(listOf(e1))

        val result = controller.getOrgAdmins("o1")

        assertEquals(1, result.size)
        assertNull(result[0].loginPassword)
        assertNull(result[0].securityPassword)
        assertNull(result[0].sessionKey)
        assertNull(result[0].authenticationKey)
        assertEquals("admin", result[0].username)
        assertEquals("bcrypt-sec", e1.securityPassword)
        verify(service).getOrgAdmins("o1")
    }

    @Test
    fun getOrgAdmins_emptyResult() {
        whenCalled(service.getOrgAdmins("o1")).thenReturn(emptyList())
        assertTrue(controller.getOrgAdmins("o1").isEmpty())
    }

    @Test
    fun getChildOrgs_delegatesAndPreservesOrder() {
        val c1 = orgCacheEntry("c1")
        val c2 = orgCacheEntry("c2")
        val children = listOf(c1, c2)
        whenCalled(service.getChildOrgs("o1")).thenReturn(children)
        val result = controller.getChildOrgs("o1")
        assertSame(children, result)
        assertEquals(listOf("c1", "c2"), result.map { it.id })
        verify(service).getChildOrgs("o1")
    }

    @Test
    fun getChildOrgs_emptyResult() {
        whenCalled(service.getChildOrgs("o1")).thenReturn(emptyList())
        assertTrue(controller.getChildOrgs("o1").isEmpty())
    }

    @Test
    fun getParentOrg_delegates() {
        val parent = orgCacheEntry("p1")
        whenCalled(service.getParentOrg("o1")).thenReturn(parent)
        assertSame(parent, controller.getParentOrg("o1"))
        verify(service).getParentOrg("o1")
    }

    @Test
    fun getParentOrg_nullWhenNoParent() {
        whenCalled(service.getParentOrg("root")).thenReturn(null)
        assertNull(controller.getParentOrg("root"))
        verify(service).getParentOrg("root")
    }

    private fun orgCacheEntry(id: String) = UserOrgCacheEntry(
        id = id,
        name = "name-$id",
        shortName = null,
        tenantId = "t1",
        parentId = null,
        orgTypeDictCode = null,
        sortNum = 0,
        remark = null,
        active = true,
        builtIn = false,
        createUserId = null,
        createUserName = null,
        createTime = null,
        updateUserId = null,
        updateUserName = null,
        updateTime = null,
    )

}
