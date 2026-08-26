package io.kudos.ms.auth.core.provider.service.iservice

import io.kudos.ms.auth.common.provider.vo.IdentityProviderDescriptor

interface IIdentityProviderCatalogService {
    fun getAvailableProviders(tenantId: String): List<IdentityProviderDescriptor>
}
