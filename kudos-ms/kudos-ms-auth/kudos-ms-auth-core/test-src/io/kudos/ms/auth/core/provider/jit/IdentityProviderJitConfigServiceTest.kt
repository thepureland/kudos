package io.kudos.ms.auth.core.provider.jit

import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.jit.dao.AuthIdentityProviderJitConfigDao
import io.kudos.ms.auth.core.provider.jit.model.IdentityProviderJitConfigException
import io.kudos.ms.auth.core.provider.jit.model.IdentityProviderJitConfigSaveCommand
import io.kudos.ms.auth.core.provider.jit.model.po.AuthIdentityProviderJitConfig
import io.kudos.ms.auth.core.provider.jit.service.impl.IdentityProviderJitConfigService
import io.kudos.ms.auth.core.provider.model.po.AuthIdentityProvider
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.org.service.iservice.IUserOrgService
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class IdentityProviderJitConfigServiceTest {
    private val dao = mock(AuthIdentityProviderJitConfigDao::class.java)
    private val providerDao = mock(AuthIdentityProviderDao::class.java)
    private val orgService = mock(IUserOrgService::class.java)
    private val userService = mock(IUserAccountService::class.java)
    private val service = IdentityProviderJitConfigService(dao, providerDao, orgService, userService)

    @Test
    fun absentRowReturnsSafeEffectiveDefaults() {
        arrangeProvider()
        `when`(dao.get("provider-1")).thenReturn(null)

        val config = service.getEffective("provider-1", "tenant-1")

        assertFalse(config.configured)
        assertFalse(config.requireVerifiedEmail)
        assertEquals("EXTERNAL_USERNAME_HASHED", config.usernameStrategy.name)
    }

    @Test
    fun saveNormalizesAndValidatesTenantOwnedDefaults() {
        arrangeProvider()
        `when`(dao.get("provider-1")).thenReturn(null)
        val org = mock(UserOrgCacheEntry::class.java)
        `when`(org.active).thenReturn(true)
        `when`(org.tenantId).thenReturn("tenant-1")
        `when`(orgService.getOrgRecord("org-1")).thenReturn(org)
        val supervisor = UserAccount { id = "supervisor-1"; tenantId = "tenant-1"; active = true }
        `when`(userService.get("supervisor-1")).thenReturn(supervisor)
        `when`(dao.insert(any(AuthIdentityProviderJitConfig::class.java) ?: fallbackPo())).thenReturn("provider-1")

        val result = service.save(command())

        assertTrue(result.configured)
        assertTrue(result.requireVerifiedEmail)
        assertEquals(listOf("*.example.com", "xn--r8jz45g.xn--zckzah"), result.allowedEmailDomains)
        assertEquals("ja_JP", result.defaultLocale)
        assertEquals("Asia/Tokyo", result.defaultTimezone)
        assertEquals("JPY", result.defaultCurrency)
        val captor = ArgumentCaptor.forClass(AuthIdentityProviderJitConfig::class.java)
        verify(dao).insert(captor.capture() ?: fallbackPo())
        assertEquals("admin-1", captor.value.createUserId)
        assertEquals("Approved JIT policy", captor.value.updateReason)
    }

    @Test
    fun saveRejectsCrossTenantOrganization() {
        arrangeProvider()
        val org = mock(UserOrgCacheEntry::class.java)
        `when`(org.active).thenReturn(true)
        `when`(org.tenantId).thenReturn("tenant-2")
        `when`(orgService.getOrgRecord("org-1")).thenReturn(org)

        val error = assertFailsWith<IdentityProviderJitConfigException> { service.save(command()) }

        assertEquals("EXTERNAL_JIT_DEFAULT_ORG_TENANT_MISMATCH", error.errorCode)
    }

    private fun arrangeProvider() {
        `when`(providerDao.get("provider-1")).thenReturn(AuthIdentityProvider {
            id = "provider-1"
            tenantId = "tenant-1"
            templateId = "template-1"
            code = "google-main"
            displayName = "Google"
            clientId = "client"
            jitPolicy = "JIT_CREATE"
            linkPolicy = "BOUND_ONLY"
            active = true
        })
    }

    private fun command() = IdentityProviderJitConfigSaveCommand(
        providerId = "provider-1",
        tenantId = "tenant-1",
        usernameStrategy = "email_local_part_hashed",
        requireVerifiedEmail = false,
        allowedEmailDomains = listOf("*.Example.COM", "例え.テスト"),
        defaultOrgId = "org-1",
        defaultSupervisorId = "supervisor-1",
        accountTypeDictCode = "EXT",
        accountStatusDictCode = "NEW",
        defaultLocale = "ja-jp",
        defaultTimezone = "Asia/Tokyo",
        defaultCurrency = "jpy",
        actorUserId = "admin-1",
        operationReason = "Approved JIT policy",
    )

    private fun fallbackPo() = AuthIdentityProviderJitConfig { id = "fallback" }
}
