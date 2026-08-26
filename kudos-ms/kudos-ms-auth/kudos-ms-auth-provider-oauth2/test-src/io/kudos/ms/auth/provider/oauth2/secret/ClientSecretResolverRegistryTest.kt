package io.kudos.ms.auth.provider.oauth2.secret

import io.kudos.ms.auth.provider.oauth2.init.ExternalLoginProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

internal class ClientSecretResolverRegistryTest {

    @Test
    fun successfulCacheIsOptInAndVerifyAlwaysBypassesIt() {
        val resolver = CountingResolver()
        val properties = ExternalLoginProperties().apply { secretCacheTtlSeconds = 60 }
        val registry = ClientSecretResolverRegistry(listOf(resolver), properties)

        assertEquals("secret-1", registry.resolve("test:client"))
        assertEquals("secret-1", registry.resolve("test:client"))
        assertEquals(1, resolver.resolveCount)

        assertEquals(ClientSecretResolutionStatus.RESOLVED, registry.verify("test:client").status)
        assertEquals(2, resolver.resolveCount)

        registry.invalidate("test:client")
        assertEquals(1, resolver.invalidateCount)
        assertEquals("secret-3", registry.resolve("test:client"))
        assertEquals(3, resolver.resolveCount)
    }

    @Test
    fun defaultCacheIsDisabledSoRotationIsImmediatelyVisible() {
        val resolver = CountingResolver()
        val registry = ClientSecretResolverRegistry(listOf(resolver))

        assertEquals("secret-1", registry.resolve("test:client"))
        assertEquals("secret-2", registry.resolve("test:client"))
    }

    @Test
    fun verificationReturnsStableNonSensitiveStatuses() {
        assertEquals(
            ClientSecretResolutionStatus.NOT_CONFIGURED,
            ClientSecretResolverRegistry(emptyList()).verify(null).status,
        )
        assertEquals(
            ClientSecretResolutionStatus.INVALID_REFERENCE,
            ClientSecretResolverRegistry(emptyList()).verify("Vault:path").status,
        )
        assertEquals(
            ClientSecretResolutionStatus.UNSUPPORTED_SCHEME,
            ClientSecretResolverRegistry(emptyList()).verify("vault:path").status,
        )
        val ambiguous = ClientSecretResolverRegistry(listOf(CountingResolver(), CountingResolver()))
        assertEquals(ClientSecretResolutionStatus.AMBIGUOUS_RESOLVER, ambiguous.verify("test:path").status)

        val denied = ClientSecretResolverRegistry(listOf(statusResolver(ClientSecretResolutionStatus.POLICY_DENIED)))
        assertEquals(ClientSecretResolutionStatus.POLICY_DENIED, denied.verify("test:path").status)
        assertNull(denied.resolve("test:path"))

        val failed = ClientSecretResolverRegistry(listOf(object : IClientSecretResolver {
            override fun supports(reference: String) = true
            override fun resolve(reference: String): String = error("backend details must not escape")
        }))
        assertEquals(ClientSecretResolutionStatus.RESOLVER_ERROR, failed.verify("test:path").status)
    }

    @Test
    fun cacheTtlHasAConservativeUpperBound() {
        val properties = ExternalLoginProperties().apply { secretCacheTtlSeconds = 3601 }
        val registry = ClientSecretResolverRegistry(listOf(CountingResolver()), properties)

        assertFailsWith<IllegalArgumentException> { registry.resolve("test:path") }
    }

    private fun statusResolver(status: ClientSecretResolutionStatus) = object : IClientSecretResolver {
        override fun supports(reference: String) = true
        override fun resolve(reference: String): String? = throw ClientSecretResolutionException(status)
    }

    private class CountingResolver : IClientSecretResolver {
        var resolveCount = 0
        var invalidateCount = 0

        override fun supports(reference: String) = reference.startsWith("test:")

        override fun resolve(reference: String): String {
            resolveCount++
            return "secret-$resolveCount"
        }

        override fun invalidate(reference: String) {
            invalidateCount++
        }
    }
}
