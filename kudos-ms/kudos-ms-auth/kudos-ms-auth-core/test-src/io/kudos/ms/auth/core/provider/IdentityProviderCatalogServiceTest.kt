package io.kudos.ms.auth.core.provider

import io.kudos.ms.auth.common.provider.enums.ExternalProtocolEnum
import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.dao.AuthProviderTemplateDao
import io.kudos.ms.auth.core.provider.model.po.AuthIdentityProvider
import io.kudos.ms.auth.core.provider.model.po.AuthProviderTemplate
import io.kudos.ms.auth.core.provider.service.impl.IdentityProviderCatalogService
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class IdentityProviderCatalogServiceTest {
    private val providerDao = mock(AuthIdentityProviderDao::class.java)
    private val templateDao = mock(AuthProviderTemplateDao::class.java)
    private val service = IdentityProviderCatalogService(providerDao, templateDao)

    private fun provider(id: String, templateId: String, name: String) = AuthIdentityProvider {
        this.id = id
        tenantId = "tenant-1"
        this.templateId = templateId
        code = id
        displayName = name
        clientId = "client-$id"
        clientSecretRef = "env:NEVER_EXPOSE_THIS"
        jitPolicy = "DISABLED"
        linkPolicy = "BOUND_ONLY"
        active = true
    }

    private fun template(id: String, activeValue: Boolean = true) = AuthProviderTemplate {
        this.id = id
        code = id.uppercase()
        protocol = "OIDC"
        subjectClaim = "sub"
        active = activeValue
        logoUri = "https://cdn.example.com/$id.svg"
    }

    @Test
    fun catalog_returnsOnlyActiveTemplateAndNeverReturnsClientConfiguration() {
        val line = provider("line", "tpl-line", "LINE")
        val google = provider("google", "tpl-google", "Google")
        val hidden = provider("hidden", "tpl-hidden", "Hidden")
        `when`(providerDao.findActiveByTenantId("tenant-1")).thenReturn(listOf(line, hidden, google))
        `when`(templateDao.get("tpl-line")).thenReturn(template("tpl-line"))
        `when`(templateDao.get("tpl-google")).thenReturn(template("tpl-google"))
        `when`(templateDao.get("tpl-hidden")).thenReturn(template("tpl-hidden", false))

        val result = service.getAvailableProviders(" tenant-1 ")

        assertEquals(listOf("google", "line"), result.map { it.id })
        assertTrue(result.all { it.protocol == ExternalProtocolEnum.OIDC })
        assertTrue(result.toString().contains("NEVER_EXPOSE_THIS").not())
        assertTrue(result.toString().contains("client-google").not())
    }
}
