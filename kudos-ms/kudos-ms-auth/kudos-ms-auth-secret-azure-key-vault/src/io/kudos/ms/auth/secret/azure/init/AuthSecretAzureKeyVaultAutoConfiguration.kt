package io.kudos.ms.auth.secret.azure.init

import com.azure.security.keyvault.secrets.SecretClient
import io.kudos.context.init.IComponentInitializer
import io.kudos.ms.auth.secret.azure.AzureKeyVaultClientSecretResolver
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(SecretClient::class)
@ConditionalOnProperty(
    prefix = "kudos.ms.auth.external-login.azure-key-vault",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(AzureKeyVaultClientSecretProperties::class)
open class AuthSecretAzureKeyVaultAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean(AzureKeyVaultClientSecretResolver::class)
    open fun azureKeyVaultClientSecretResolver(
        clientProvider: ObjectProvider<SecretClient>,
        properties: AzureKeyVaultClientSecretProperties,
    ) = AzureKeyVaultClientSecretResolver(clientProvider.orderedStream().toList(), properties)

    override fun getComponentName() = "kudos-ms-auth-secret-azure-key-vault"
}
