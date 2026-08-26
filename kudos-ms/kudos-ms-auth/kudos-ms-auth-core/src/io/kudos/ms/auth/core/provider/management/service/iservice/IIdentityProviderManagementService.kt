package io.kudos.ms.auth.core.provider.management.service.iservice

import io.kudos.ms.auth.core.provider.management.model.IdentityProviderCreateCommand
import io.kudos.ms.auth.core.provider.management.model.IdentityProviderSetActiveCommand
import io.kudos.ms.auth.core.provider.management.model.IdentityProviderUpdateCommand
import io.kudos.ms.auth.core.provider.management.model.ManagedIdentityProvider
import io.kudos.ms.auth.core.provider.management.model.ManagedProviderTemplate

interface IIdentityProviderManagementService {
    fun listTemplates(): List<ManagedProviderTemplate>
    fun list(tenantId: String): List<ManagedIdentityProvider>
    fun get(providerId: String, tenantId: String): ManagedIdentityProvider
    fun create(command: IdentityProviderCreateCommand): ManagedIdentityProvider
    fun update(command: IdentityProviderUpdateCommand): ManagedIdentityProvider
    fun setActive(command: IdentityProviderSetActiveCommand): ManagedIdentityProvider
}
