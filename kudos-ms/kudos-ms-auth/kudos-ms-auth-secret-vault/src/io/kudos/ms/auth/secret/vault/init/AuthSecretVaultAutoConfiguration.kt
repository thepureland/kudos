package io.kudos.ms.auth.secret.vault.init

import io.kudos.context.init.IComponentInitializer
import io.kudos.ms.auth.secret.vault.VaultClientSecretResolver
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.vault.core.VaultOperations

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(VaultOperations::class)
@ConditionalOnProperty(
    prefix = "kudos.ms.auth.external-login.vault",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(VaultClientSecretProperties::class)
open class AuthSecretVaultAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean(VaultClientSecretResolver::class)
    open fun vaultClientSecretResolver(
        vaultOperationsProvider: ObjectProvider<VaultOperations>,
        properties: VaultClientSecretProperties,
    ) = VaultClientSecretResolver(vaultOperationsProvider, properties)

    override fun getComponentName() = "kudos-ms-auth-secret-vault"
}
