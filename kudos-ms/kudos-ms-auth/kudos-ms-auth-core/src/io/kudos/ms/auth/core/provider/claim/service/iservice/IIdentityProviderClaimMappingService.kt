package io.kudos.ms.auth.core.provider.claim.service.iservice

import io.kudos.ms.auth.core.provider.claim.model.EffectiveIdentityProviderClaimMapping
import io.kudos.ms.auth.core.provider.claim.model.IdentityProviderClaimMappingSaveCommand

interface IIdentityProviderClaimMappingService {
    fun getEffective(providerId: String, tenantId: String): EffectiveIdentityProviderClaimMapping
    fun save(command: IdentityProviderClaimMappingSaveCommand): EffectiveIdentityProviderClaimMapping
}
