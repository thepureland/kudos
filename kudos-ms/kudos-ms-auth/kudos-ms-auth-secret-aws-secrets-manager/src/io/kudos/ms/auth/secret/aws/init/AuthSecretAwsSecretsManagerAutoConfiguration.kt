package io.kudos.ms.auth.secret.aws.init

import io.kudos.context.init.IComponentInitializer
import io.kudos.ms.auth.secret.aws.AwsSecretsManagerClientSecretResolver
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(SecretsManagerClient::class)
@ConditionalOnProperty(
    prefix = "kudos.ms.auth.external-login.aws-secrets-manager",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(AwsSecretsManagerClientSecretProperties::class)
open class AuthSecretAwsSecretsManagerAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean(AwsSecretsManagerClientSecretResolver::class)
    open fun awsSecretsManagerClientSecretResolver(
        clientProvider: ObjectProvider<SecretsManagerClient>,
        properties: AwsSecretsManagerClientSecretProperties,
    ) = AwsSecretsManagerClientSecretResolver(clientProvider, properties)

    override fun getComponentName() = "kudos-ms-auth-secret-aws-secrets-manager"
}
