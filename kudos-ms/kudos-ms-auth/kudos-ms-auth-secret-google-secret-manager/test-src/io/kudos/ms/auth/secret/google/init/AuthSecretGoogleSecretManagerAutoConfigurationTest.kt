package io.kudos.ms.auth.secret.google.init

import io.kudos.ms.auth.secret.google.GoogleSecretManagerClientSecretResolver
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AuthSecretGoogleSecretManagerAutoConfigurationTest {
    private val runner = ApplicationContextRunner()
        .withUserConfiguration(AuthSecretGoogleSecretManagerAutoConfiguration::class.java)

    @Test
    fun resolverIsRegisteredWithoutOpeningAGoogleConnectionAtStartup() {
        runner.run { context ->
            assertEquals(1, context.getBeanNamesForType(GoogleSecretManagerClientSecretResolver::class.java).size)
        }
    }

    @Test
    fun resolverMayBeDisabledWhenAnotherModuleOwnsTheScheme() {
        runner.withPropertyValues(
            "kudos.ms.auth.external-login.google-secret-manager.enabled=false"
        ).run { context ->
            assertEquals(0, context.getBeanNamesForType(GoogleSecretManagerClientSecretResolver::class.java).size)
        }
    }

    @Test
    fun projectsSecretPrefixesAndVersionBindFromConfiguration() {
        runner.withPropertyValues(
            "kudos.ms.auth.external-login.google-secret-manager.allowed-project-ids=company-prod,123456789012",
            "kudos.ms.auth.external-login.google-secret-manager.allowed-secret-id-prefixes=kudos-auth-,partner-",
            "kudos.ms.auth.external-login.google-secret-manager.version=42",
        ).run { context ->
            val properties = context.getBean(GoogleSecretManagerClientSecretProperties::class.java)
            assertEquals(listOf("company-prod", "123456789012"), properties.allowedProjectIds)
            assertEquals(listOf("kudos-auth-", "partner-"), properties.allowedSecretIdPrefixes)
            assertEquals("42", properties.version)
        }
    }
}
