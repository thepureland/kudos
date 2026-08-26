package io.kudos.ms.auth.core.provider.jit.service.iservice

import io.kudos.ms.auth.core.provider.jit.model.EffectiveIdentityProviderJitConfig
import io.kudos.ms.auth.core.provider.jit.model.IdentityProviderJitConfigSaveCommand

interface IIdentityProviderJitConfigService {

    fun getEffective(providerId: String, tenantId: String): EffectiveIdentityProviderJitConfig

    fun save(command: IdentityProviderJitConfigSaveCommand): EffectiveIdentityProviderJitConfig
}
