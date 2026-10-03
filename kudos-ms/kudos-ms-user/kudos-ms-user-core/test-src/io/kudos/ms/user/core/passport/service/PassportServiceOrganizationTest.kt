package io.kudos.ms.user.core.passport.service

import io.kudos.ability.security.common.init.SecurityCommonAutoConfiguration
import io.kudos.base.security.PasswordKit
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.passport.enums.PassportLoginStatusEnum
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.security.IAccountCredentialStore
import io.kudos.ms.user.core.account.security.PasswordPolicyContext
import io.kudos.ms.user.core.account.security.PasswordPurpose
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.login.service.iservice.IUserLogLoginService
import io.kudos.ms.user.core.org.spi.IOrganizationMemberPolicy
import io.kudos.ms.user.core.passport.service.impl.PassportService
import io.kudos.ms.user.core.security.accessGuard
import io.kudos.ms.user.core.security.directoryGate
import io.kudos.ms.user.core.security.organizationMode
import io.kudos.ms.user.core.security.setField
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Pure tests of the organization-mode sign-in decision of [PassportService]: an organization account
 * gets a session only when the member policy lets it in, and only after its credentials are verified.
 */
internal class PassportServiceOrganizationTest {

    private val userAccountService = mock(IUserAccountService::class.java)
    private val policy = mock(IOrganizationMemberPolicy::class.java)
    private val passwordEncoder = SecurityCommonAutoConfiguration().passwordEncoder()
    private val plain = "secret-pwd-123"
    private val hash = PasswordKit.hash(plain, strength = 4)

    private fun build(
        modeEnabled: Boolean = true,
        withGate: Boolean = true,
        credentialStores: List<IAccountCredentialStore> = emptyList(),
    ): PassportService = PassportService(
        userAccountService,
        mock(UserAccountDao::class.java),
        mock(IUserLogLoginService::class.java),
        passwordEncoder,
        credentialStores = credentialStores,
    ).also { service ->
        if (withGate) setField(service, "directoryGate", directoryGate(organizationMode(modeEnabled), accessGuard(), policy))
    }

    private fun entry(id: String, tenantId: String, organizationId: String?, loginPassword: String? = hash) = UserAccountCacheEntry(
        id = id, username = "alice", tenantId = tenantId, loginPassword = loginPassword, securityPassword = null,
        accountTypeDictCode = "10", accountStatusDictCode = null, defaultLocale = "en", defaultTimezone = "UTC",
        defaultCurrency = "USD", lastLoginTime = null, lastLoginIp = null, lastLogoutTime = null, loginErrorTimes = 0,
        securityPasswordErrorTimes = null, sessionKey = null, authenticationKey = null, orgId = "dept-1",
        supervisorId = null, remark = null, freezeType = null, freezeStartTime = null, freezeEndTime = null,
        freezeTitle = null, active = true, builtIn = false, createUserId = null, createUserName = null, createTime = null,
        updateUserId = null, updateUserName = null, updateTime = null, organizationId = organizationId,
    )

    private fun stubLookup(tenantId: String, account: UserAccountCacheEntry) {
        whenCalled(userAccountService.getUserByTenantIdAndUsername(tenantId, "alice")).thenReturn(account)
    }

    private fun login(tenantId: String, password: String = plain) = build().login(PassportLoginRequest(tenantId, "alice", password))

    @Test
    fun organizationAccountDeniedEntryGetsTenantAccessDeniedAfterPasswordVerification() {
        stubLookup("tenant-a", entry("m1", "", "org-1"))
        whenCalled(policy.canSignIn("tenant-a", "m1")).thenReturn(false)
        val result = login("tenant-a")
        assertEquals(PassportLoginStatusEnum.TENANT_ACCESS_DENIED, result.status)
        assertNull(result.userInfo)
        verify(policy).canSignIn("tenant-a", "m1")
        verify(userAccountService, never()).resetLoginErrorTimes(anyString())
    }

    @Test
    fun wrongPasswordOfAnOrganizationAccountNeverReachesTheEntryDecision() {
        stubLookup("tenant-a", entry("m1", "", "org-1"))
        val result = login("tenant-a", password = "wrong-password-1")
        assertEquals(PassportLoginStatusEnum.WRONG_PASSWORD, result.status)
        verifyNoInteractions(policy)
    }

    @Test
    fun organizationAccountAllowedEntryWorksInTheLoginTenant() {
        stubLookup("tenant-b", entry("m1", "", "org-1"))
        whenCalled(policy.canSignIn("tenant-b", "m1")).thenReturn(true)
        val result = login("tenant-b")
        assertEquals(PassportLoginStatusEnum.SUCCESS, result.status)
        val info = assertNotNull(result.userInfo)
        assertEquals("m1", info.id)
        assertEquals("tenant-b", info.tenantId)
        assertEquals("org-1", info.organizationId)
        verify(userAccountService).resetLoginErrorTimes("m1")
    }

    @Test
    fun organizationAccountIsDeniedWithoutAGateOrWithModeOff() {
        stubLookup("tenant-a", entry("m1", "", "org-1"))
        whenCalled(policy.canSignIn("tenant-a", "m1")).thenReturn(true)
        val noGate = build(withGate = false).login(PassportLoginRequest("tenant-a", "alice", plain))
        assertEquals(PassportLoginStatusEnum.TENANT_ACCESS_DENIED, noGate.status)
        val modeOff = build(modeEnabled = false).login(PassportLoginRequest("tenant-a", "alice", plain))
        assertEquals(PassportLoginStatusEnum.TENANT_ACCESS_DENIED, modeOff.status)
    }

    @Test
    fun legacyAccountIsUnaffectedByTheEntryDecision() {
        stubLookup("t1", entry("u1", "t1", null))
        whenCalled(policy.canSignIn(anyString(), anyString())).thenReturn(false)
        val result = login("t1")
        assertEquals(PassportLoginStatusEnum.SUCCESS, result.status)
        val info = assertNotNull(result.userInfo)
        assertEquals("t1", info.tenantId)
        assertNull(info.organizationId)
        verifyNoInteractions(policy)
    }

    @Test
    fun legacyAccountSignsInWithoutAnyGate() {
        stubLookup("t1", entry("u1", "t1", null))
        val result = build(withGate = false).login(PassportLoginRequest("t1", "alice", plain))
        assertEquals(PassportLoginStatusEnum.SUCCESS, result.status)
    }

    @Test
    fun organizationCredentialsAreFiledUnderTheAccountsOwnTenantNotTheLoginTenant() {
        val store = mock(IAccountCredentialStore::class.java)
        val organizationContext = PasswordPolicyContext(PasswordPurpose.LOGIN, "m1", "alice", "")
        whenCalled(store.verifyPassword(plain, organizationContext)).thenReturn(true)
        stubLookup("tenant-a", entry("m1", "", "org-1", loginPassword = ""))
        whenCalled(policy.canSignIn("tenant-a", "m1")).thenReturn(true)

        val result = build(credentialStores = listOf(store)).login(PassportLoginRequest("tenant-a", "alice", plain))

        assertEquals(PassportLoginStatusEnum.SUCCESS, result.status)
        verify(store).verifyPassword(plain, organizationContext)
        verify(store).upgradePasswordEncodingIfNeeded(plain, organizationContext)
    }

    @Test
    fun legacyCredentialsStayFiledUnderTheLoginTenant() {
        val store = mock(IAccountCredentialStore::class.java)
        val legacyContext = PasswordPolicyContext(PasswordPurpose.LOGIN, "u1", "alice", "t1")
        whenCalled(store.verifyPassword(plain, legacyContext)).thenReturn(true)
        stubLookup("t1", entry("u1", "t1", null, loginPassword = ""))

        val result = build(credentialStores = listOf(store)).login(PassportLoginRequest("t1", "alice", plain))

        assertEquals(PassportLoginStatusEnum.SUCCESS, result.status)
        val captor = ArgumentCaptor.forClass(PasswordPolicyContext::class.java)
        verify(store).verifyPassword(eqPlain(), captor.capture() ?: legacyContext)
        assertEquals("t1", captor.value.tenantId)
    }

    private fun eqPlain(): String = org.mockito.ArgumentMatchers.eq(plain) ?: plain
}
