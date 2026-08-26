package io.kudos.ms.auth.secret.google.init

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import io.kudos.context.init.IComponentInitializer
import io.kudos.ms.auth.secret.google.GoogleSecretManagerClientSecretResolver
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(SecretManagerServiceClient::class)
@ConditionalOnProperty(
    prefix = "kudos.ms.auth.external-login.google-secret-manager",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(GoogleSecretManagerClientSecretProperties::class)
open class AuthSecretGoogleSecretManagerAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean(GoogleSecretManagerClientSecretResolver::class)
    open fun googleSecretManagerClientSecretResolver(
        clientProvider: ObjectProvider<SecretManagerServiceClient>,
        properties: GoogleSecretManagerClientSecretProperties,
    ) = GoogleSecretManagerClientSecretResolver(clientProvider, properties)

    override fun getComponentName() = "kudos-ms-auth-secret-google-secret-manager"
}
