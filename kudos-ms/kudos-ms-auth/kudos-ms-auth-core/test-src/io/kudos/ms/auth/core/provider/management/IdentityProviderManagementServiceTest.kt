package io.kudos.ms.auth.core.provider.management

import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.dao.AuthProviderTemplateDao
import io.kudos.ms.auth.core.provider.management.model.IdentityProviderCreateCommand
import io.kudos.ms.auth.core.provider.management.model.IdentityProviderManagementException
import io.kudos.ms.auth.core.provider.management.model.IdentityProviderUpdateCommand
import io.kudos.ms.auth.core.provider.management.service.impl.IdentityProviderManagementService
import io.kudos.ms.auth.core.provider.model.po.AuthIdentityProvider
import io.kudos.ms.auth.core.provider.model.po.AuthProviderTemplate
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class IdentityProviderManagementServiceTest {
    private val providerDao = mock(AuthIdentityProviderDao::class.java)
    private val templateDao = mock(AuthProviderTemplateDao::class.java)
    private val service = IdentityProviderManagementService(providerDao, templateDao)

    @Test
    fun createNormalizesConfigurationAndPersistsAuditWithoutExposingSecretReference() {
        arrangeTemplate()
        `when`(providerDao.findByTenantIdAndCode("tenant-1", "google-main")).thenReturn(null)
        `when`(providerDao.insert(any(AuthIdentityProvider::class.java) ?: fallbackProvider()))
            .thenReturn("provider-1")

        val result = service.create(createCommand())

        assertEquals("provider-1", result.id)
        assertEquals("google-main", result.code)
        assertEquals(listOf("openid", "profile", "email"), result.scopes)
        assertTrue(result.clientSecretConfigured)
        val captor = ArgumentCaptor.forClass(AuthIdentityProvider::class.java)
        verify(providerDao).insert(captor.capture() ?: fallbackProvider())
        assertEquals("vault:auth/google", captor.value.clientSecretRef)
        assertEquals("admin-1", captor.value.createUserId)
        assertEquals("Register corporate login", captor.value.updateReason)
    }

    @Test
    fun updatePreservesSecretReferenceWhenReplacementIsAbsent() {
        val provider = provider()
        `when`(providerDao.get("provider-1")).thenReturn(provider)
        arrangeTemplate()
        `when`(providerDao.update(provider)).thenReturn(true)

        val result = service.update(
            IdentityProviderUpdateCommand(
                providerId = "provider-1",
                tenantId = "tenant-1",
                displayName = "Google Workforce",
                issuer = null,
                clientId = "client-new",
                clientSecretRef = null,
                clearClientSecretRef = false,
                scopes = listOf("openid", "email"),
                jitPolicy = "invite_only",
                linkPolicy = "manual_confirm",
                actorUserId = "admin-2",
                operationReason = "Rotate client metadata",
            )
        )

        assertTrue(result.clientSecretConfigured)
        assertEquals("vault:auth/google", provider.clientSecretRef)
        assertEquals("INVITE_ONLY", provider.jitPolicy)
        assertEquals("MANUAL_CONFIRM", provider.linkPolicy)
        assertEquals("admin-2", provider.updateUserId)
    }

    @Test
    fun updateClearsSecretReferenceOnlyWhenExplicitlyRequested() {
        val provider = provider()
        `when`(providerDao.get("provider-1")).thenReturn(provider)
        arrangeTemplate()
        `when`(providerDao.update(provider)).thenReturn(true)

        val result = service.update(
            IdentityProviderUpdateCommand(
                providerId = "provider-1",
                tenantId = "tenant-1",
                displayName = "Google Public Client",
                issuer = null,
                clientId = "public-client",
                clientSecretRef = null,
                clearClientSecretRef = true,
                scopes = listOf("openid", "profile"),
                jitPolicy = "DISABLED",
                linkPolicy = "BOUND_ONLY",
                actorUserId = "admin-2",
                operationReason = "Convert to PKCE public client",
            )
        )

        assertEquals(null, provider.clientSecretRef)
        assertEquals(false, result.clientSecretConfigured)
    }

    @Test
    fun rejectsUnsafeSecretValueUnsupportedLinkPolicyAndCrossTenantAccess() {
        arrangeTemplate()
        `when`(providerDao.findByTenantIdAndCode("tenant-1", "google-main")).thenReturn(null)
        assertError("EXTERNAL_PROVIDER_SECRET_REF_INVALID") {
            service.create(createCommand().copy(clientSecretRef = "plain-secret"))
        }
        assertError("EXTERNAL_PROVIDER_SECRET_REF_INVALID") {
            service.create(createCommand().copy(clientSecretRef = "Vault:auth/google"))
        }
        assertError("EXTERNAL_PROVIDER_LINK_POLICY_NOT_SUPPORTED") {
            service.create(createCommand().copy(linkPolicy = "MATCH_VERIFIED_EMAIL"))
        }
        `when`(providerDao.get("provider-1")).thenReturn(provider())
        assertError("EXTERNAL_PROVIDER_TENANT_MISMATCH") {
            service.get("provider-1", "tenant-2")
        }
    }

    private fun arrangeTemplate() {
        `when`(templateDao.get("template-google")).thenReturn(template())
    }

    private fun createCommand() = IdentityProviderCreateCommand(
        tenantId = " tenant-1 ",
        templateId = "template-google",
        code = " Google-Main ",
        displayName = " Google Workforce ",
        issuer = null,
        clientId = " client-id ",
        clientSecretRef = " vault:auth/google ",
        scopes = listOf("openid profile", "email", "email"),
        jitPolicy = "disabled",
        linkPolicy = "bound_only",
        active = false,
        actorUserId = "admin-1",
        operationReason = " Register corporate login ",
    )

    private fun template() = AuthProviderTemplate {
        id = "template-google"
        code = "GOOGLE"
        protocol = "OIDC"
        issuer = "https://accounts.google.com"
        subjectClaim = "sub"
        defaultScopes = "openid profile email"
        active = true
    }

    private fun provider() = AuthIdentityProvider {
        id = "provider-1"
        tenantId = "tenant-1"
        templateId = "template-google"
        code = "google-main"
        displayName = "Google"
        clientId = "client-id"
        clientSecretRef = "vault:auth/google"
        scopes = "openid profile email"
        jitPolicy = "DISABLED"
        linkPolicy = "BOUND_ONLY"
        active = true
    }

    private fun assertError(code: String, block: () -> Unit) {
        val error = assertFailsWith<IdentityProviderManagementException>(block = block)
        assertEquals(code, error.errorCode)
    }

    private fun fallbackProvider() = AuthIdentityProvider { id = "fallback" }
}
