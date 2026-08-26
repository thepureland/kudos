package io.kudos.ms.auth.secret.azure

import com.azure.security.keyvault.secrets.SecretClient
import com.azure.security.keyvault.secrets.models.KeyVaultSecret
import io.kudos.ms.auth.provider.oauth2.init.ExternalLoginProperties
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolutionException
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolutionStatus
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolverRegistry
import io.kudos.ms.auth.secret.azure.init.AzureKeyVaultClientSecretProperties
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

internal class AzureKeyVaultClientSecretResolverTest {

    @Test
    fun readsLatestSecretFromTheClientMatchingTheConfiguredVaultAlias() {
        val other = client("https://other.vault.azure.net", "wrong")
        val selected = client("https://COMPANY-PROD.vault.azure.net/", "azure-secret")
        val resolver = resolver(listOf(other, selected))

        assertEquals(
            "azure-secret",
            resolver.resolve("azure-kv:company-prod/kudos-auth-google"),
        )

        verify(selected).getSecret("kudos-auth-google")
        verify(other, never()).getSecret("kudos-auth-google")
    }

    @Test
    fun readsExactTopLevelJsonField() {
        val client = client(
            value = """{"client-secret":"line-secret","nested":{"client-secret":"wrong"}}""",
            secretName = "kudos-auth-line",
        )

        assertEquals(
            "line-secret",
            resolver(listOf(client)).resolve("azure-kv:company-prod/kudos-auth-line#client-secret"),
        )
    }

    @Test
    fun absentFieldAndBlankValueAreNotFound() {
        val client = mock(SecretClient::class.java)
        `when`(client.vaultUrl).thenReturn(DEFAULT_VAULT_URL)
        `when`(client.getSecret("kudos-auth-json")).thenReturn(secret("kudos-auth-json", """{"other":"value"}"""))
        `when`(client.getSecret("kudos-auth-blank")).thenReturn(secret("kudos-auth-blank", "   "))
        val resolver = resolver(listOf(client))

        assertNull(resolver.resolve("azure-kv:company-prod/kudos-auth-json#client-secret"))
        assertNull(resolver.resolve("azure-kv:company-prod/kudos-auth-blank"))
    }

    @Test
    fun urlPathAndVersionInjectionAreRejectedBeforeCallingAzure() {
        val client = mock(SecretClient::class.java)
        val resolver = resolver(listOf(client))
        listOf(
            "azure-kv:/kudos-auth-google",
            "azure-kv:company-prod/../kudos-auth-google",
            "azure-kv:company-prod/kudos-auth-google/old-version",
            "azure-kv:https://company-prod.vault.azure.net/kudos-auth-google",
            "azure-kv:company-prod/kudos-auth-google?version=old",
            "azure-kv:company-prod/kudos-auth-google#",
            "azure-kv:company-prod/kudos-auth-google#field#other",
        ).forEach { reference ->
            assertStatus(ClientSecretResolutionStatus.INVALID_REFERENCE) { resolver.resolve(reference) }
        }
        verify(client, never()).getSecret("kudos-auth-google")
    }

    @Test
    fun vaultAliasAndSecretPrefixPoliciesAreBothRequired() {
        assertStatus(ClientSecretResolutionStatus.POLICY_DENIED) {
            resolver().resolve("azure-kv:other-prod/kudos-auth-google")
        }
        assertStatus(ClientSecretResolutionStatus.POLICY_DENIED) {
            resolver().resolve("azure-kv:company-prod/other-google")
        }
        val noVaults = properties().apply { vaultUrls = emptyMap() }
        assertStatus(ClientSecretResolutionStatus.POLICY_DENIED) {
            resolver(properties = noVaults).resolve("azure-kv:company-prod/kudos-auth-google")
        }
        val noPrefixes = properties().apply { allowedSecretNamePrefixes = emptyList() }
        assertStatus(ClientSecretResolutionStatus.POLICY_DENIED) {
            resolver(properties = noPrefixes).resolve("azure-kv:company-prod/kudos-auth-google")
        }
    }

    @Test
    fun invalidConfiguredVaultEndpointsFailClosed() {
        listOf(
            "http://company-prod.vault.azure.net",
            "https://user@company-prod.vault.azure.net",
            "https://company-prod.vault.azure.net:8443",
            "https://company-prod.vault.azure.net/secrets",
            "https://company-prod.vault.azure.net?api-version=1",
        ).forEach { url ->
            val invalid = properties().apply { vaultUrls = mapOf("company-prod" to url) }
            assertStatus(ClientSecretResolutionStatus.RESOLVER_ERROR) {
                resolver(properties = invalid).resolve("azure-kv:company-prod/kudos-auth-google")
            }
        }
    }

    @Test
    fun missingAndDuplicateMatchingClientsCollapseToSafeStatus() {
        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            registry(resolver()).verify("azure-kv:company-prod/kudos-auth-google").status,
        )
        val first = client(value = "first")
        val second = client(value = "second")
        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            registry(resolver(listOf(first, second)))
                .verify("azure-kv:company-prod/kudos-auth-google").status,
        )
        verify(first, never()).getSecret("kudos-auth-google")
        verify(second, never()).getSecret("kudos-auth-google")
    }

    @Test
    fun apiErrorsOversizedValuesAndMismatchedResponsesFailClosed() {
        val apiFailure = mock(SecretClient::class.java)
        `when`(apiFailure.vaultUrl).thenReturn(DEFAULT_VAULT_URL)
        `when`(apiFailure.getSecret("kudos-auth-api")).thenThrow(
            IllegalStateException("vault URL and Azure request details must not escape")
        )
        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            registry(resolver(listOf(apiFailure))).verify("azure-kv:company-prod/kudos-auth-api").status,
        )

        val oversized = client(value = "x".repeat(65_537), secretName = "kudos-auth-large")
        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            registry(resolver(listOf(oversized))).verify("azure-kv:company-prod/kudos-auth-large").status,
        )

        val mismatch = client(value = "secret", secretName = "different-name")
        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            registry(resolver(listOf(mismatch))).verify("azure-kv:company-prod/kudos-auth-google").status,
        )
    }

    @Test
    fun invalidJsonAndNonStringJsonFieldFailClosed() {
        val invalid = client(value = "not-json", secretName = "kudos-auth-invalid")
        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            registry(resolver(listOf(invalid)))
                .verify("azure-kv:company-prod/kudos-auth-invalid#client-secret").status,
        )
        val nonString = client(value = """{"client-secret":{"nested":true}}""", secretName = "kudos-auth-object")
        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            registry(resolver(listOf(nonString)))
                .verify("azure-kv:company-prod/kudos-auth-object#client-secret").status,
        )
    }

    @Test
    fun registryInvalidationReadsLatestAgainAfterRotation() {
        val client = mock(SecretClient::class.java)
        `when`(client.vaultUrl).thenReturn(DEFAULT_VAULT_URL)
        `when`(client.getSecret("kudos-auth-google"))
            .thenReturn(secret("kudos-auth-google", "old-secret"))
            .thenReturn(secret("kudos-auth-google", "new-secret"))
        val cache = ExternalLoginProperties().apply { secretCacheTtlSeconds = 60 }
        val registry = ClientSecretResolverRegistry(listOf(resolver(listOf(client))), cache)

        assertEquals("old-secret", registry.resolve("azure-kv:company-prod/kudos-auth-google"))
        assertEquals("old-secret", registry.resolve("azure-kv:company-prod/kudos-auth-google"))
        registry.invalidate("azure-kv:company-prod/kudos-auth-google")
        assertEquals("new-secret", registry.resolve("azure-kv:company-prod/kudos-auth-google"))
        verify(client, times(2)).getSecret("kudos-auth-google")
    }

    @Test
    fun serverMayPinAnAzureVersionWhileReferencesCannot() {
        val version = "0123456789abcdef0123456789ABCDEF"
        val client = mock(SecretClient::class.java)
        `when`(client.vaultUrl).thenReturn(DEFAULT_VAULT_URL)
        `when`(client.getSecret("kudos-auth-google", version))
            .thenReturn(secret("kudos-auth-google", "pinned-secret"))
        val properties = properties().apply { this.version = version }

        assertEquals(
            "pinned-secret",
            resolver(listOf(client), properties).resolve("azure-kv:company-prod/kudos-auth-google"),
        )
        verify(client).getSecret("kudos-auth-google", version)

        val invalidVersion = properties().apply { this.version = "previous" }
        assertStatus(ClientSecretResolutionStatus.RESOLVER_ERROR) {
            resolver(listOf(client), invalidVersion).resolve("azure-kv:company-prod/kudos-auth-google")
        }
    }

    private fun resolver(
        clients: List<SecretClient> = emptyList(),
        properties: AzureKeyVaultClientSecretProperties = properties(),
    ) = AzureKeyVaultClientSecretResolver(clients, properties)

    private fun properties() = AzureKeyVaultClientSecretProperties().apply {
        vaultUrls = mapOf("company-prod" to DEFAULT_VAULT_URL)
    }

    private fun client(
        vaultUrl: String = DEFAULT_VAULT_URL,
        value: String,
        secretName: String = "kudos-auth-google",
    ): SecretClient {
        val client = mock(SecretClient::class.java)
        `when`(client.vaultUrl).thenReturn(vaultUrl)
        `when`(client.getSecret(secretName)).thenReturn(secret(secretName, value))
        return client
    }

    private fun secret(name: String, value: String) = KeyVaultSecret(name, value)

    private fun registry(resolver: AzureKeyVaultClientSecretResolver) =
        ClientSecretResolverRegistry(listOf(resolver))

    private fun assertStatus(status: ClientSecretResolutionStatus, block: () -> Unit) {
        val error = assertFailsWith<ClientSecretResolutionException>(block = block)
        assertEquals(status, error.status)
    }

    private companion object {
        const val DEFAULT_VAULT_URL = "https://company-prod.vault.azure.net/"
    }
}
