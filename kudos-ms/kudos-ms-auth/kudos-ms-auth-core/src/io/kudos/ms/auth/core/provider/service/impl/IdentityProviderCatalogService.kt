package io.kudos.ms.auth.core.provider.service.impl

import io.kudos.ms.auth.common.provider.enums.ExternalProtocolEnum
import io.kudos.ms.auth.common.provider.vo.IdentityProviderDescriptor
import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.dao.AuthProviderTemplateDao
import io.kudos.ms.auth.core.provider.service.iservice.IIdentityProviderCatalogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Builds the secret-free login-page catalog from tenant instances and platform templates. */
@Service
@Transactional(readOnly = true)
open class IdentityProviderCatalogService(
    private val identityProviderDao: AuthIdentityProviderDao,
    private val providerTemplateDao: AuthProviderTemplateDao,
) : IIdentityProviderCatalogService {

    override fun getAvailableProviders(tenantId: String): List<IdentityProviderDescriptor> {
        require(tenantId.isNotBlank()) { "Tenant id must not be blank" }
        return identityProviderDao.findActiveByTenantId(tenantId.trim()).mapNotNull { provider ->
            val template = providerTemplateDao.get(provider.templateId)
                ?.takeIf { it.active }
                ?: return@mapNotNull null
            val protocol = runCatching { ExternalProtocolEnum.valueOf(template.protocol.uppercase()) }
                .getOrNull()
                ?: return@mapNotNull null
            IdentityProviderDescriptor(
                id = provider.id,
                code = provider.code,
                displayName = provider.displayName,
                protocol = protocol,
                logoUri = template.logoUri,
            )
        }.sortedBy { it.displayName.lowercase() }
    }
}
