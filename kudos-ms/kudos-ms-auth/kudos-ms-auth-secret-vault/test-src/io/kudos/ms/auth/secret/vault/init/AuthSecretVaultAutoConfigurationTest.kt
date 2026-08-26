package io.kudos.ms.auth.secret.vault.init

import io.kudos.ms.auth.secret.vault.VaultClientSecretResolver
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AuthSecretVaultAutoConfigurationTest {
    private val runner = ApplicationContextRunner()
        .withUserConfiguration(AuthSecretVaultAutoConfiguration::class.java)

    @Test
    fun resolverIsAvailableWithoutForcingAVaultConnectionAtStartup() {
        runner.run { context ->
            assertEquals(1, context.getBeanNamesForType(VaultClientSecretResolver::class.java).size)
        }
    }

    @Test
    fun deploymentMayDisableResolverWhenAnotherModuleOwnsScheme() {
        runner.withPropertyValues("kudos.ms.auth.external-login.vault.enabled=false").run { context ->
            assertEquals(0, context.getBeanNamesForType(VaultClientSecretResolver::class.java).size)
        }
    }

    @Test
    fun mountVersionPathPolicyAndDefaultKeyBindFromConfiguration() {
        runner.withPropertyValues(
            "kudos.ms.auth.external-login.vault.mount=identity-secrets",
            "kudos.ms.auth.external-login.vault.backend-version=1",
            "kudos.ms.auth.external-login.vault.allowed-path-prefixes=oauth,partners",
            "kudos.ms.auth.external-login.vault.default-key=oauth-client-secret",
        ).run { context ->
            val properties = context.getBean(VaultClientSecretProperties::class.java)
            assertEquals("identity-secrets", properties.mount)
            assertEquals(1, properties.backendVersion)
            assertEquals(listOf("oauth", "partners"), properties.allowedPathPrefixes)
            assertEquals("oauth-client-secret", properties.defaultKey)
        }
    }
}
