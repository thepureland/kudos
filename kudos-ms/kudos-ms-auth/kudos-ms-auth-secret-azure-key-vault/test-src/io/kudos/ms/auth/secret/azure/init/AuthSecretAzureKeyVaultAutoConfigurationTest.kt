package io.kudos.ms.auth.secret.azure.init

import io.kudos.ms.auth.secret.azure.AzureKeyVaultClientSecretResolver
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AuthSecretAzureKeyVaultAutoConfigurationTest {
    private val runner = ApplicationContextRunner()
        .withUserConfiguration(AuthSecretAzureKeyVaultAutoConfiguration::class.java)

    @Test
    fun resolverIsRegisteredWithoutOpeningAnAzureConnectionAtStartup() {
        runner.run { context ->
            assertEquals(1, context.getBeanNamesForType(AzureKeyVaultClientSecretResolver::class.java).size)
        }
    }

    @Test
    fun resolverMayBeDisabledWhenAnotherModuleOwnsTheScheme() {
        runner.withPropertyValues(
            "kudos.ms.auth.external-login.azure-key-vault.enabled=false"
        ).run { context ->
            assertEquals(0, context.getBeanNamesForType(AzureKeyVaultClientSecretResolver::class.java).size)
        }
    }

    @Test
    fun vaultAliasesSecretPrefixesAndVersionBindFromConfiguration() {
        val version = "0123456789abcdef0123456789abcdef"
        runner.withPropertyValues(
            "kudos.ms.auth.external-login.azure-key-vault.vault-urls.company-prod=https://company-prod.vault.azure.net/",
            "kudos.ms.auth.external-login.azure-key-vault.vault-urls.partner-cn=https://partner.vault.azure.cn/",
            "kudos.ms.auth.external-login.azure-key-vault.allowed-secret-name-prefixes=kudos-auth-,partner-",
            "kudos.ms.auth.external-login.azure-key-vault.version=$version",
        ).run { context ->
            val properties = context.getBean(AzureKeyVaultClientSecretProperties::class.java)
            assertEquals(
                mapOf(
                    "company-prod" to "https://company-prod.vault.azure.net/",
                    "partner-cn" to "https://partner.vault.azure.cn/",
                ),
                properties.vaultUrls,
            )
            assertEquals(listOf("kudos-auth-", "partner-"), properties.allowedSecretNamePrefixes)
            assertEquals(version, properties.version)
        }
    }
}
