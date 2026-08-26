package io.kudos.ms.auth.core.provider.claim

import io.kudos.ms.auth.core.provider.claim.dao.AuthIdentityProviderClaimMappingDao
import io.kudos.ms.auth.core.provider.claim.model.IdentityProviderClaimMappingException
import io.kudos.ms.auth.core.provider.claim.model.IdentityProviderClaimMappingSaveCommand
import io.kudos.ms.auth.core.provider.claim.model.po.AuthIdentityProviderClaimMapping
import io.kudos.ms.auth.core.provider.claim.service.impl.IdentityProviderClaimMappingService
import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.dao.AuthProviderTemplateDao
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class IdentityProviderClaimMappingServiceTest {
    private val dao = mock(AuthIdentityProviderClaimMappingDao::class.java)
    private val providerDao = mock(AuthIdentityProviderDao::class.java)
    private val templateDao = mock(AuthProviderTemplateDao::class.java)
    private val service = IdentityProviderClaimMappingService(dao, providerDao, templateDao)

    @Test
    fun absentMappingReturnsProtocolSafeDefaults() {
        arrangeScope()
        `when`(dao.get("provider-1")).thenReturn(null)

        val mapping = service.getEffective("provider-1", "tenant-1")

        assertFalse(mapping.configured)
        assertEquals(listOf("sub"), mapping.subjectClaims)
        assertEquals(listOf("preferred_username", "login"), mapping.usernameClaims)
        assertEquals(listOf("picture", "avatar_url"), mapping.avatarClaims)
    }

    @Test
    fun saveNormalizesOrderedNestedPathsAndPersistsAudit() {
        arrangeScope()
        `when`(dao.get("provider-1")).thenReturn(null)
        `when`(dao.insert(any(AuthIdentityProviderClaimMapping::class.java) ?: fallbackMapping()))
            .thenReturn("provider-1")

        val result = service.save(command())

        assertTrue(result.configured)
        assertEquals(listOf("profile.username", "login"), result.usernameClaims)
        val captor = ArgumentCaptor.forClass(AuthIdentityProviderClaimMapping::class.java)
        verify(dao).insert(captor.capture() ?: fallbackMapping())
        assertEquals("profile.username,login", captor.value.usernameClaims)
        assertEquals("profiles.0.email,email", captor.value.emailClaims)
        assertEquals("admin-1", captor.value.createUserId)
        assertEquals("Map corporate claims", captor.value.updateReason)
    }

    @Test
    fun rejectsMutableOidcSubjectInvalidPathAndCrossTenantAccess() {
        arrangeScope()
        assertError("EXTERNAL_CLAIM_MAPPING_OIDC_SUBJECT_IMMUTABLE") {
            service.save(command().copy(subjectClaims = listOf("identity.id")))
        }
        assertError("EXTERNAL_CLAIM_MAPPING_PATH_INVALID") {
            service.save(command().copy(usernameClaims = listOf("profile[0].name")))
        }
        assertError("EXTERNAL_PROVIDER_TENANT_MISMATCH") {
            service.getEffective("provider-1", "tenant-2")
        }
    }

    private fun arrangeScope() {
        `when`(providerDao.get("provider-1")).thenReturn(AuthIdentityProvider {
            id = "provider-1"
            tenantId = "tenant-1"
            templateId = "template-1"
        })
        `when`(templateDao.get("template-1")).thenReturn(AuthProviderTemplate {
            id = "template-1"
            code = "GENERIC_OIDC"
            protocol = "OIDC"
            subjectClaim = "provider_specific_subject"
            active = true
        })
    }

    private fun command() = IdentityProviderClaimMappingSaveCommand(
        providerId = "provider-1",
        tenantId = "tenant-1",
        subjectClaims = listOf(" sub ", "sub"),
        usernameClaims = listOf(" profile.username ", "login"),
        displayNameClaims = listOf("profile.display_name"),
        emailClaims = listOf("profiles.0.email", "email"),
        emailVerifiedClaims = listOf("email_verified"),
        phoneClaims = emptyList(),
        phoneVerifiedClaims = emptyList(),
        avatarClaims = listOf("profile.avatar.url"),
        localeClaims = listOf("locale"),
        unionIdClaims = emptyList(),
        actorUserId = "admin-1",
        operationReason = " Map corporate claims ",
    )

    private fun assertError(code: String, block: () -> Unit) {
        val error = assertFailsWith<IdentityProviderClaimMappingException>(block = block)
        assertEquals(code, error.errorCode)
    }

    private fun fallbackMapping() = AuthIdentityProviderClaimMapping { id = "fallback" }
}
