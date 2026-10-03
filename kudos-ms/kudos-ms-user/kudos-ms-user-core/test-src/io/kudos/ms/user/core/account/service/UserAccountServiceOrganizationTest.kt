package io.kudos.ms.user.core.account.service

import io.kudos.ability.security.common.init.SecurityCommonAutoConfiguration
import io.kudos.base.query.Criteria
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import io.kudos.ms.user.core.account.event.UserAuthenticationInvalidated
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.security.DefaultPasswordPolicy
import io.kudos.ms.user.core.account.security.IPasswordHistory
import io.kudos.ms.user.core.account.security.PasswordPolicyProperties
import io.kudos.ms.user.core.account.service.impl.UserAccountService
import io.kudos.ms.user.core.org.cache.OrgIdsByUserIdCache
import io.kudos.ms.user.core.org.cache.UserOrgHashCache
import io.kudos.ms.user.core.org.dao.UserOrgDao
import io.kudos.ms.user.core.org.service.iservice.IOrganizationOwnershipService
import io.kudos.ms.user.core.org.spi.IOrganizationMemberPolicy
import io.kudos.ms.user.core.security.PLATFORM_TENANT
import io.kudos.ms.user.core.security.accessGuard
import io.kudos.ms.user.core.security.department
import io.kudos.ms.user.core.security.directoryGate
import io.kudos.ms.user.core.security.legacyOrg
import io.kudos.ms.user.core.security.organizationMode
import io.kudos.ms.user.core.security.organizationRoot
import io.kudos.ms.user.core.security.setField
import io.kudos.ms.user.core.security.signIn
import io.kudos.ms.user.core.security.signOut
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when` as whenCalled
import org.springframework.context.ApplicationEventPublisher
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pure tests of the organization-mode paths of [UserAccountService]: organization-owned accounts,
 * member-policy checks on lifecycle and credential maintenance, and organization-aware lookup.
 */
internal class UserAccountServiceOrganizationTest {

    private val dao = mock(UserAccountDao::class.java)
    private val userOrgDao = mock(UserOrgDao::class.java)
    private val userAccountHashCache = mock(UserAccountHashCache::class.java)
    private val policy = mock(IOrganizationMemberPolicy::class.java)
    private val ownership = mock(IOrganizationOwnershipService::class.java)
    private val events = mutableListOf<Any>()
    private val password = "correct horse battery staple"

    private fun build(modeEnabled: Boolean = true): UserAccountService {
        val guard = accessGuard(ownership)
        return UserAccountService(
            dao,
            ApplicationEventPublisher { events.add(it) },
            DefaultPasswordPolicy(PasswordPolicyProperties()),
            listOf(mock(IPasswordHistory::class.java)),
            SecurityCommonAutoConfiguration().passwordEncoder(),
        ).also { svc ->
            setField(svc, "userOrgHashCache", mock(UserOrgHashCache::class.java))
            setField(svc, "userAccountHashCache", userAccountHashCache)
            setField(svc, "orgIdsByUserIdCache", mock(OrgIdsByUserIdCache::class.java))
            setField(svc, "userOrgDao", userOrgDao)
            setField(svc, "tenantAccess", guard)
            setField(svc, "directoryGate", directoryGate(organizationMode(modeEnabled), guard, policy))
            setField(svc, "ownership", ownership)
        }
    }

    private fun anyArg(): Any = ArgumentMatchers.any() ?: Any()
    private fun anyMapArg(): Map<String, *> = ArgumentMatchers.anyMap<String, Any?>() ?: emptyMap<String, Any?>()

    private fun member(id: String, organizationId: String = "org-1") = UserAccount {
        this.id = id
        this.organizationId = organizationId
        tenantId = ""
        username = id
        loginPassword = ""
        supervisorId = ""
        active = true
    }

    private fun legacyAccount(id: String, tenantId: String) = UserAccount {
        this.id = id
        this.tenantId = tenantId
        username = id
        loginPassword = ""
        supervisorId = ""
        active = true
    }

    private fun newAccount(tenantId: String = "tenant-a", organizationId: String? = null, orgId: String? = null) = UserAccount {
        username = "bob"
        this.tenantId = tenantId
        this.organizationId = organizationId
        this.orgId = orgId
        loginPassword = ""
        supervisorId = ""
    }

    @AfterTest
    fun clear() {
        signOut()
        events.clear()
    }

    // ---- insert ----

    @Test
    fun organizationPrincipalCreatesAnOrganizationOwnedAccount() {
        whenCalled(userOrgDao.get("org-1")).thenReturn(organizationRoot("org-1"))
        val po = newAccount(tenantId = "tenant-a")
        whenCalled(dao.insert(po)).thenReturn("new-id")
        signIn("admin", "tenant-a", organizationId = "org-1")
        assertEquals("new-id", build().insert(po))
        assertEquals("org-1", po.organizationId)
        assertEquals("", po.tenantId)
        verify(policy).assertCanManageMember("org-1", null)
    }

    @Test
    fun organizationAccountMayBeFiledInADepartmentOfItsOrganization() {
        whenCalled(userOrgDao.get("org-1")).thenReturn(organizationRoot("org-1"))
        whenCalled(userOrgDao.get("dept-a")).thenReturn(department("dept-a", "org-1"))
        val po = newAccount(orgId = "dept-a")
        whenCalled(dao.insert(po)).thenReturn("new-id")
        signIn("admin", "tenant-a", organizationId = "org-1")
        assertEquals("new-id", build().insert(po))
        assertEquals("dept-a", po.orgId)
    }

    @Test
    fun departmentOfAnotherOrganizationIsRefused() {
        whenCalled(userOrgDao.get("org-1")).thenReturn(organizationRoot("org-1"))
        whenCalled(userOrgDao.get("dept-b")).thenReturn(department("dept-b", "org-2"))
        signIn("admin", "tenant-a", organizationId = "org-1")
        assertFailsWith<IllegalArgumentException> { build().insert(newAccount(orgId = "dept-b")) }
        signOut()
        // Trusted work naming org-1 but a department of org-2.
        val e = assertFailsWith<IllegalArgumentException> { build().insert(newAccount(organizationId = "org-1", orgId = "dept-b")) }
        assertEquals("Account and department must belong to the same organization", e.message)
        verify(dao, never()).insert(anyArg())
    }

    @Test
    fun legacyDepartmentIsRefusedForAnOrganizationAccount() {
        whenCalled(userOrgDao.get("org-1")).thenReturn(organizationRoot("org-1"))
        whenCalled(userOrgDao.get("legacy")).thenReturn(legacyOrg("legacy", "tenant-a"))
        assertFailsWith<IllegalArgumentException> { build().insert(newAccount(organizationId = "org-1", orgId = "legacy")) }
        verify(dao, never()).insert(anyArg())
    }

    @Test
    fun supervisorOfAnotherOwnerIsRefused() {
        whenCalled(userOrgDao.get("org-1")).thenReturn(organizationRoot("org-1"))
        whenCalled(dao.get("boss")).thenReturn(member("boss", organizationId = "org-2"))
        val po = newAccount(organizationId = "org-1").apply { supervisorId = "boss" }
        val e = assertFailsWith<IllegalArgumentException> { build().insert(po) }
        assertEquals("Account and supervisor must belong to the same organization", e.message)
        verify(dao, never()).insert(anyArg())
    }

    @Test
    fun organizationPrincipalCannotCreateAccountsInAnotherOrganization() {
        whenCalled(userOrgDao.get("org-2")).thenReturn(organizationRoot("org-2"))
        signIn("admin", "tenant-a", organizationId = "org-1")
        assertFailsWith<IllegalArgumentException> { build().insert(newAccount(organizationId = "org-2")) }
        verify(dao, never()).insert(anyArg())
        verifyNoInteractions(policy)
    }

    @Test
    fun legacyPrincipalCannotCreateOrganizationAccounts() {
        whenCalled(userOrgDao.get("org-1")).thenReturn(organizationRoot("org-1"))
        signIn("u1", "tenant-a")
        assertFailsWith<IllegalArgumentException> { build().insert(newAccount(organizationId = "org-1")) }
        verify(dao, never()).insert(anyArg())
    }

    @Test
    fun organizationAccountNeedsAnActiveRoot() {
        whenCalled(userOrgDao.get("org-off")).thenReturn(organizationRoot("org-off", active = false))
        whenCalled(userOrgDao.get("dept-a")).thenReturn(department("dept-a", "org-1"))
        assertEquals("ORGANIZATION_DISABLED", assertFailsWith<IllegalArgumentException> { build().insert(newAccount(organizationId = "org-off")) }.message)
        assertEquals("ORGANIZATION_NOT_FOUND", assertFailsWith<IllegalArgumentException> { build().insert(newAccount(organizationId = "dept-a")) }.message)
        verify(dao, never()).insert(anyArg())
    }

    @Test
    fun policyRefusalStopsTheInsert() {
        doThrow(IllegalArgumentException("NO")).`when`(policy).assertCanManageMember("org-1", null)
        signIn("m1", "tenant-a", organizationId = "org-1")
        assertFailsWith<IllegalArgumentException> { build().insert(newAccount()) }
        verify(dao, never()).insert(anyArg())
    }

    @Test
    fun legacyAccountsInsertAsBeforeWhenModeIsOffAndOnlyInPlatformTenantsWhenOn() {
        val po = newAccount(tenantId = "tenant-a")
        whenCalled(dao.insert(po)).thenReturn("legacy-id")
        assertEquals("legacy-id", build(modeEnabled = false).insert(po))
        assertEquals("tenant-a", po.tenantId)
        assertNull(po.organizationId)

        assertEquals("ORGANIZATION_REQUIRED", assertFailsWith<IllegalArgumentException> { build().insert(newAccount("tenant-a")) }.message)
        val platform = newAccount(tenantId = PLATFORM_TENANT)
        whenCalled(dao.insert(platform)).thenReturn("platform-id")
        assertEquals("platform-id", build().insert(platform))
        verifyNoInteractions(policy)
    }

    // ---- update ----

    @Test
    fun updateCannotMoveAnAccountToAnotherOrganizationAndPinsOwnership() {
        whenCalled(dao.get("m1")).thenReturn(member("m1"))
        whenCalled(dao.get("legacy")).thenReturn(legacyAccount("legacy", "tenant-a"))
        val service = build()
        assertFailsWith<IllegalArgumentException> { service.update(UserAccount { id = "m1"; organizationId = "org-2" }) }
        assertFailsWith<IllegalArgumentException> { service.update(UserAccount { id = "legacy"; organizationId = "org-1" }) }
        verify(dao, never()).update(anyArg())

        whenCalled(dao.update(anyArg())).thenReturn(true)
        val form = UserAccount { id = "m1"; remark = "x" }
        assertTrue(service.update(form))
        assertEquals("org-1", form.organizationId)
        assertEquals("", form.tenantId)
        verify(policy).assertCanManageMember("org-1", "m1")
    }

    // ---- lifecycle: end of membership ----

    @Test
    fun disablingAnOrganizationAccountEndsMembershipThroughThePolicy() {
        whenCalled(dao.get("m2")).thenReturn(member("m2"))
        whenCalled(dao.update(anyArg())).thenReturn(true)
        signIn("admin", "tenant-a", organizationId = "org-1")
        assertTrue(build().updateActive("m2", false))
        verify(policy).assertCanEndMembership("org-1", "m2")
        val invalidated = events.filterIsInstance<UserAuthenticationInvalidated>().single()
        assertEquals("org-1", invalidated.organizationId)
        assertEquals(UserAuthenticationInvalidated.Reason.ACCOUNT_DISABLED, invalidated.reason)
    }

    @Test
    fun enablingAnOrganizationAccountIsMemberManagement() {
        whenCalled(dao.get("m2")).thenReturn(member("m2"))
        whenCalled(dao.update(anyArg())).thenReturn(true)
        assertTrue(build().updateActive("m2", true))
        verify(policy).assertCanManageMember("org-1", "m2")
        verify(policy, never()).assertCanEndMembership(anyString(), anyString())
    }

    @Test
    fun policyRefusalKeepsTheAccountEnabled() {
        whenCalled(dao.get("m2")).thenReturn(member("m2"))
        doThrow(IllegalArgumentException("LAST_ORGANIZATION_ADMIN")).`when`(policy).assertCanEndMembership("org-1", "m2")
        assertFailsWith<IllegalArgumentException> { build().updateActive("m2", false) }
        verify(dao, never()).update(anyArg())
        assertTrue(events.isEmpty())
    }

    @Test
    fun deletingAnOrganizationAccountEndsMembershipThroughThePolicy() {
        whenCalled(dao.get("m2")).thenReturn(member("m2"))
        whenCalled(dao.deleteById("m2")).thenReturn(true)
        whenCalled(dao.batchDeleteCriteria(ArgumentMatchers.any(Criteria::class.java) ?: Criteria())).thenReturn(1)
        signIn("admin", "tenant-a", organizationId = "org-1")
        assertTrue(build().deleteById("m2"))
        verify(policy).assertCanEndMembership("org-1", "m2")
        val deleted = events.filterIsInstance<UserAccountDeleted>().single()
        assertEquals("org-1", deleted.organizationId)
    }

    @Test
    fun deleteIsRefusedWhenThePolicyRefuses() {
        whenCalled(dao.get("m2")).thenReturn(member("m2"))
        doThrow(IllegalArgumentException("NO")).`when`(policy).assertCanEndMembership("org-1", "m2")
        assertFailsWith<IllegalArgumentException> { build().deleteById("m2") }
        verify(dao, never()).deleteById(anyString())
        verify(dao, never()).batchDeleteCriteria(ArgumentMatchers.any(Criteria::class.java) ?: Criteria())
    }

    @Test
    fun batchDeleteAsksThePolicyForEveryOrganizationAccount() {
        val ids = listOf("m2", "m3", "legacy")
        whenCalled(dao.getByIds(ids)).thenReturn(listOf(member("m2"), member("m3"), legacyAccount("legacy", PLATFORM_TENANT)))
        whenCalled(dao.batchDelete(ids)).thenReturn(3)
        whenCalled(dao.batchDeleteCriteria(ArgumentMatchers.any(Criteria::class.java) ?: Criteria())).thenReturn(3)
        build().batchDelete(ids)
        verify(policy).assertCanEndMembership("org-1", "m2")
        verify(policy).assertCanEndMembership("org-1", "m3")
        val items = events.filterIsInstance<UserAccountBatchDeleted>().single().items
        assertEquals(listOf("org-1", "org-1", null), items.map { it.organizationId })
    }

    @Test
    fun organizationPrincipalCannotEndMembershipInAnotherOrganization() {
        whenCalled(dao.get("x1")).thenReturn(member("x1", organizationId = "org-2"))
        signIn("admin", "tenant-a", organizationId = "org-1")
        assertFailsWith<IllegalArgumentException> { build().updateActive("x1", false) }
        assertFailsWith<IllegalArgumentException> { build().deleteById("x1") }
        verify(dao, never()).update(anyArg())
        verifyNoInteractions(policy)
    }

    @Test
    fun freezingAnOrganizationAccountEndsMembershipThroughThePolicy() {
        whenCalled(dao.get("m2")).thenReturn(member("m2"))
        whenCalled(dao.updateProperties(anyString(), anyMapArg())).thenReturn(true)
        assertTrue(build().freezeAccount("m2", "manual", "t", null, null, null))
        verify(policy).assertCanEndMembership("org-1", "m2")
        assertEquals("org-1", events.filterIsInstance<UserAuthenticationInvalidated>().single().organizationId)
    }

    @Test
    fun legacyLifecycleNeverConsultsThePolicy() {
        whenCalled(dao.get("legacy")).thenReturn(legacyAccount("legacy", "tenant-a"))
        whenCalled(dao.update(anyArg())).thenReturn(true)
        build(modeEnabled = false).updateActive("legacy", false)
        build(modeEnabled = false).updateActive("legacy", true)
        verifyNoInteractions(policy)
    }

    // ---- credential maintenance ----

    @Test
    fun ownPasswordResetIsNotMemberManagement() {
        whenCalled(dao.get("m1")).thenReturn(member("m1"))
        whenCalled(dao.update(anyArg())).thenReturn(true)
        signIn("m1", "tenant-a", organizationId = "org-1")
        assertTrue(build().resetPassword("m1", password))
        assertTrue(build().resetSecurityPassword("m1", password))
        verifyNoInteractions(policy)
    }

    @Test
    fun resettingSomeoneElsesPasswordIsMemberManagement() {
        whenCalled(dao.get("m2")).thenReturn(member("m2"))
        whenCalled(dao.update(anyArg())).thenReturn(true)
        signIn("m1", "tenant-a", organizationId = "org-1")
        assertTrue(build().resetPassword("m2", password))
        verify(policy).assertCanManageMember("org-1", "m2")
        val invalidated = events.filterIsInstance<UserAuthenticationInvalidated>().single()
        assertEquals(UserAuthenticationInvalidated.Reason.LOGIN_PASSWORD_CHANGED, invalidated.reason)
    }

    @Test
    fun refusedPasswordResetWritesNothing() {
        whenCalled(dao.get("m2")).thenReturn(member("m2"))
        doThrow(IllegalArgumentException("NO")).`when`(policy).assertCanManageMember("org-1", "m2")
        signIn("m1", "tenant-a", organizationId = "org-1")
        assertFailsWith<IllegalArgumentException> { build().resetPassword("m2", password) }
        assertFailsWith<IllegalArgumentException> { build().resetSecurityPassword("m2", password) }
        assertFailsWith<IllegalArgumentException> { build().cleanAuthKey("m2") }
        verify(dao, never()).update(anyArg())
        verify(dao, never()).updateProperties(anyString(), anyMapArg())
    }

    @Test
    fun clearingOwnAuthKeyIsNotMemberManagementButSomeoneElsesIs() {
        whenCalled(dao.get("m1")).thenReturn(member("m1"))
        whenCalled(dao.get("m2")).thenReturn(member("m2"))
        whenCalled(dao.updateProperties(anyString(), anyMapArg())).thenReturn(true)
        signIn("m1", "tenant-a", organizationId = "org-1")
        build().cleanAuthKey("m1")
        verifyNoInteractions(policy)
        build().cleanAuthKey("m2")
        verify(policy).assertCanManageMember("org-1", "m2")
    }

    @Test
    fun legacyPasswordResetNeverConsultsThePolicy() {
        whenCalled(dao.get("legacy")).thenReturn(legacyAccount("legacy", "tenant-a"))
        whenCalled(dao.update(anyArg())).thenReturn(true)
        signIn("operator", "tenant-a")
        assertTrue(build().resetPassword("legacy", password))
        verifyNoInteractions(policy)
    }

    // ---- lookup ----

    private fun cached(id: String): UserAccountCacheEntry =
        mock(UserAccountCacheEntry::class.java).also { whenCalled(it.id).thenReturn(id) }

    @Test
    fun lookupInAnOrganizationTenantUsesTheOrganizationsAccounts() {
        val hit = cached("m1")
        val full = cached("m1")
        whenCalled(ownership.organizationIdForTenant("tenant-a")).thenReturn("org-1")
        whenCalled(userAccountHashCache.getUserByOrganizationIdAndUsername("org-1", "bob")).thenReturn(hit)
        whenCalled(userAccountHashCache.getUserById("m1")).thenReturn(full)
        assertSame(full, build().getUserByTenantIdAndUsername("tenant-a", "bob"))
        verify(userAccountHashCache, never()).getUsersByTenantIdAndUsername(anyString(), anyString())
    }

    @Test
    fun lookupInAPlatformTenantStaysLegacy() {
        val hit = cached("p1")
        whenCalled(ownership.organizationIdForTenant(PLATFORM_TENANT)).thenReturn(null)
        whenCalled(userAccountHashCache.getUsersByTenantIdAndUsername(PLATFORM_TENANT, "root")).thenReturn(hit)
        whenCalled(userAccountHashCache.getUserById("p1")).thenReturn(hit)
        assertSame(hit, build().getUserByTenantIdAndUsername(PLATFORM_TENANT, "root"))
        verify(userAccountHashCache, never()).getUserByOrganizationIdAndUsername(anyString(), anyString())
    }

    @Test
    fun lookupIgnoresOrganizationsWhenModeIsOff() {
        val hit = cached("u1")
        whenCalled(ownership.organizationIdForTenant("tenant-a")).thenReturn("org-1")
        whenCalled(userAccountHashCache.getUsersByTenantIdAndUsername("tenant-a", "bob")).thenReturn(hit)
        whenCalled(userAccountHashCache.getUserById("u1")).thenReturn(hit)
        assertSame(hit, build(modeEnabled = false).getUserByTenantIdAndUsername("tenant-a", "bob"))
        verify(userAccountHashCache, never()).getUserByOrganizationIdAndUsername(anyString(), anyString())
        verify(ownership, never()).organizationIdForTenant(anyString())
    }

    @Test
    fun lookupThroughAnotherOrganizationsTenantIsRefused() {
        whenCalled(ownership.organizationIdForTenant("tenant-x")).thenReturn("org-2")
        signIn("m1", "tenant-a", organizationId = "org-1")
        assertFailsWith<IllegalArgumentException> { build().getUserByTenantIdAndUsername("tenant-x", "bob") }
        verify(userAccountHashCache, never()).getUserByOrganizationIdAndUsername(anyString(), anyString())
    }
}
