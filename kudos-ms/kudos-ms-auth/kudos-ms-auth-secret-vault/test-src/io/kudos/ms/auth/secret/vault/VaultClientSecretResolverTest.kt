package io.kudos.ms.auth.secret.vault

import io.kudos.ms.auth.provider.oauth2.init.ExternalLoginProperties
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolutionException
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolutionStatus
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolverRegistry
import io.kudos.ms.auth.secret.vault.init.VaultClientSecretProperties
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.ObjectProvider
import org.springframework.vault.core.VaultOperations
import org.springframework.vault.support.VaultResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

internal class VaultClientSecretResolverTest {

    @Test
    fun kv2ReadsPinnedMountAndUnwrapsDataMap() {
        val operations = mock(VaultOperations::class.java)
        `when`(operations.read("secret/data/oauth/tenant-a/google")).thenReturn(
            response(mapOf("data" to mapOf("client-secret" to "google-secret")))
        )
        val resolver = resolver(operations = operations)

        assertEquals("google-secret", resolver.resolve("vault:oauth/tenant-a/google"))
        verify(operations).read("secret/data/oauth/tenant-a/google")
    }

    @Test
    fun explicitFieldAndKv1AreSupported() {
        val operations = mock(VaultOperations::class.java)
        `when`(operations.read("auth-secrets/oauth/tenant-a/line")).thenReturn(
            response(mapOf("line-client-secret" to "line-secret"))
        )
        val properties = properties().apply {
            mount = "auth-secrets"
            backendVersion = 1
        }

        assertEquals(
            "line-secret",
            resolver(operations, properties).resolve("vault:oauth/tenant-a/line#line-client-secret"),
        )
    }

    @Test
    fun absentPathFieldOrDeletedKv2ValueIsNotFound() {
        val operations = mock(VaultOperations::class.java)
        `when`(operations.read("secret/data/oauth/missing")).thenReturn(null)
        `when`(operations.read("secret/data/oauth/no-field")).thenReturn(
            response(mapOf("data" to mapOf("other" to "value")))
        )
        `when`(operations.read("secret/data/oauth/deleted")).thenReturn(response(emptyMap()))
        val resolver = resolver(operations = operations)

        assertNull(resolver.resolve("vault:oauth/missing"))
        assertNull(resolver.resolve("vault:oauth/no-field"))
        assertNull(resolver.resolve("vault:oauth/deleted"))
    }

    @Test
    fun traversalEncodingAndUnsafeFieldSyntaxAreRejectedBeforeVaultCall() {
        val resolver = resolver()
        listOf(
            "vault:/oauth/google",
            "vault:oauth/../google",
            "vault:oauth/%2e%2e/google",
            "vault:oauth\\google",
            "vault:oauth/google?version=1",
            "vault:oauth/google#",
            "vault:oauth/google#secret#other",
        ).forEach { reference ->
            assertStatus(ClientSecretResolutionStatus.INVALID_REFERENCE) { resolver.resolve(reference) }
        }
    }

    @Test
    fun allowlistUsesPathSegmentBoundariesAndEmptyListDeniesAll() {
        val resolver = resolver()
        assertStatus(ClientSecretResolutionStatus.POLICY_DENIED) {
            resolver.resolve("vault:oauth2/google")
        }

        val denied = properties().apply { allowedPathPrefixes = emptyList() }
        assertStatus(ClientSecretResolutionStatus.POLICY_DENIED) {
            resolver(properties = denied).resolve("vault:oauth/google")
        }
    }

    @Test
    fun backendFailuresAndNonStringValuesCollapseToSafeStatus() {
        val operations = mock(VaultOperations::class.java)
        `when`(operations.read("secret/data/oauth/failure")).thenThrow(
            IllegalStateException("sensitive backend response")
        )
        `when`(operations.read("secret/data/oauth/wrong-type")).thenReturn(
            response(mapOf("data" to mapOf("client-secret" to mapOf("nested" to "value"))))
        )
        val registry = ClientSecretResolverRegistry(listOf(resolver(operations)))

        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            registry.verify("vault:oauth/failure").status,
        )
        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            registry.verify("vault:oauth/wrong-type").status,
        )
    }

    @Test
    fun missingVaultOperationsFailsClosedWithoutStartupFailure() {
        val registry = ClientSecretResolverRegistry(listOf(resolver()))

        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            registry.verify("vault:oauth/google").status,
        )
    }

    @Test
    fun registryCacheCanBeExplicitlyInvalidatedAfterVaultRotation() {
        val operations = mock(VaultOperations::class.java)
        `when`(operations.read("secret/data/oauth/google"))
            .thenReturn(response(mapOf("data" to mapOf("client-secret" to "old-secret"))))
            .thenReturn(response(mapOf("data" to mapOf("client-secret" to "new-secret"))))
        val cache = ExternalLoginProperties().apply { secretCacheTtlSeconds = 60 }
        val registry = ClientSecretResolverRegistry(listOf(resolver(operations)), cache)

        assertEquals("old-secret", registry.resolve("vault:oauth/google"))
        assertEquals("old-secret", registry.resolve("vault:oauth/google"))
        registry.invalidate("vault:oauth/google")
        assertEquals("new-secret", registry.resolve("vault:oauth/google"))
        verify(operations, org.mockito.Mockito.times(2)).read("secret/data/oauth/google")
    }

    @Test
    fun invalidServerConfigurationIsNeverClassifiedAsAReferenceError() {
        val invalidMount = properties().apply { mount = "../secret" }
        assertStatus(ClientSecretResolutionStatus.RESOLVER_ERROR) {
            resolver(operations = mock(VaultOperations::class.java), properties = invalidMount)
                .resolve("vault:oauth/google")
        }
        val invalidVersion = properties().apply { backendVersion = 3 }
        assertStatus(ClientSecretResolutionStatus.RESOLVER_ERROR) {
            resolver(operations = mock(VaultOperations::class.java), properties = invalidVersion)
                .resolve("vault:oauth/google")
        }
    }

    private fun resolver(
        operations: VaultOperations? = null,
        properties: VaultClientSecretProperties = properties(),
    ): VaultClientSecretResolver {
        @Suppress("UNCHECKED_CAST")
        val provider = mock(ObjectProvider::class.java) as ObjectProvider<VaultOperations>
        `when`(provider.getIfAvailable()).thenReturn(operations)
        return VaultClientSecretResolver(provider, properties)
    }

    private fun properties() = VaultClientSecretProperties()

    private fun response(data: Map<String, Any>) = VaultResponse().apply { this.data = data }

    private fun assertStatus(status: ClientSecretResolutionStatus, block: () -> Unit) {
        val error = assertFailsWith<ClientSecretResolutionException>(block = block)
        assertEquals(status, error.status)
    }
}
