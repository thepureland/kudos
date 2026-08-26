package io.kudos.ms.auth.secret.aws.init

import io.kudos.ms.auth.secret.aws.AwsSecretsManagerClientSecretResolver
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AuthSecretAwsSecretsManagerAutoConfigurationTest {
    private val runner = ApplicationContextRunner()
        .withUserConfiguration(AuthSecretAwsSecretsManagerAutoConfiguration::class.java)

    @Test
    fun resolverIsRegisteredWithoutOpeningAnAwsConnectionAtStartup() {
        runner.run { context ->
            assertEquals(1, context.getBeanNamesForType(AwsSecretsManagerClientSecretResolver::class.java).size)
        }
    }

    @Test
    fun resolverMayBeDisabledWhenAnotherModuleOwnsTheScheme() {
        runner.withPropertyValues(
            "kudos.ms.auth.external-login.aws-secrets-manager.enabled=false"
        ).run { context ->
            assertEquals(0, context.getBeanNamesForType(AwsSecretsManagerClientSecretResolver::class.java).size)
        }
    }

    @Test
    fun namespaceArnAndRotationStagePoliciesBindFromConfiguration() {
        runner.withPropertyValues(
            "kudos.ms.auth.external-login.aws-secrets-manager.allowed-secret-id-prefixes=company/auth,partners",
            "kudos.ms.auth.external-login.aws-secrets-manager.allow-arns=true",
            "kudos.ms.auth.external-login.aws-secrets-manager.allowed-arn-prefixes=" +
                "arn:aws:secretsmanager:ap-northeast-1:123456789012:secret:company/auth/",
            "kudos.ms.auth.external-login.aws-secrets-manager.version-stage=BLUE",
        ).run { context ->
            val properties = context.getBean(AwsSecretsManagerClientSecretProperties::class.java)
            assertEquals(listOf("company/auth", "partners"), properties.allowedSecretIdPrefixes)
            assertEquals(true, properties.allowArns)
            assertEquals(1, properties.allowedArnPrefixes.size)
            assertEquals("BLUE", properties.versionStage)
        }
    }
}
