package io.kudos.ms.auth.secret.google

import com.google.cloud.secretmanager.v1.AccessSecretVersionRequest
import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import com.google.cloud.secretmanager.v1.SecretPayload
import com.google.protobuf.ByteString
import io.kudos.ms.auth.provider.oauth2.init.ExternalLoginProperties
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolutionException
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolutionStatus
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolverRegistry
import io.kudos.ms.auth.secret.google.init.GoogleSecretManagerClientSecretProperties
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.ObjectProvider
import java.util.zip.CRC32C
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

internal class GoogleSecretManagerClientSecretResolverTest {

    @Test
    fun readsLatestUtf8PayloadAndVerifiesCrc32c() {
        val client = mock(SecretManagerServiceClient::class.java)
        `when`(client.accessSecretVersion(anyRequest())).thenReturn(response("google-secret"))
        val resolver = resolver(client)

        assertEquals("google-secret", resolver.resolve("gcp-sm:company-prod/kudos-auth-google"))

        val captor = ArgumentCaptor.forClass(AccessSecretVersionRequest::class.java)
        verify(client).accessSecretVersion(
            captor.capture() ?: AccessSecretVersionRequest.getDefaultInstance()
        )
        assertEquals(
            "projects/company-prod/secrets/kudos-auth-google/versions/latest",
            captor.value.name,
        )
    }

    @Test
    fun readsExactTopLevelJsonField() {
        val client = mock(SecretManagerServiceClient::class.java)
        `when`(client.accessSecretVersion(anyRequest())).thenReturn(
            response("""{"client-secret":"line-secret","nested":{"client-secret":"wrong"}}""")
        )

        assertEquals(
            "line-secret",
            resolver(client).resolve("gcp-sm:company-prod/kudos-auth-line#client-secret"),
        )
    }

    @Test
    fun absentFieldAndBlankPayloadAreNotFound() {
        val client = mock(SecretManagerServiceClient::class.java)
        `when`(client.accessSecretVersion(anyRequest()))
            .thenReturn(response("""{"other":"value"}"""))
            .thenReturn(response("   "))
        val resolver = resolver(client)

        assertNull(resolver.resolve("gcp-sm:company-prod/kudos-auth-google#client-secret"))
        assertNull(resolver.resolve("gcp-sm:company-prod/kudos-auth-google"))
    }

    @Test
    fun resourceAndVersionInjectionAreRejectedBeforeRpc() {
        val client = mock(SecretManagerServiceClient::class.java)
        val resolver = resolver(client)
        listOf(
            "gcp-sm:/kudos-auth-google",
            "gcp-sm:company-prod/../kudos-auth-google",
            "gcp-sm:company-prod/kudos-auth-google/versions/1",
            "gcp-sm:projects/company-prod/secrets/kudos-auth-google",
            "gcp-sm:company-prod/kudos-auth-google?version=1",
            "gcp-sm:company-prod/kudos-auth-google#",
            "gcp-sm:company-prod/kudos-auth-google#field#other",
        ).forEach { reference ->
            assertStatus(ClientSecretResolutionStatus.INVALID_REFERENCE) { resolver.resolve(reference) }
        }
        verify(client, never()).accessSecretVersion(anyRequest())
    }

    @Test
    fun projectAndSecretPrefixPoliciesAreBothRequired() {
        assertStatus(ClientSecretResolutionStatus.POLICY_DENIED) {
            resolver().resolve("gcp-sm:other-prod/kudos-auth-google")
        }
        assertStatus(ClientSecretResolutionStatus.POLICY_DENIED) {
            resolver().resolve("gcp-sm:company-prod/other-google")
        }
        val noProjects = properties().apply { allowedProjectIds = emptyList() }
        assertStatus(ClientSecretResolutionStatus.POLICY_DENIED) {
            resolver(properties = noProjects).resolve("gcp-sm:company-prod/kudos-auth-google")
        }
    }

    @Test
    fun missingPayloadBadChecksumInvalidUtf8AndNonStringJsonFailClosed() {
        val client = mock(SecretManagerServiceClient::class.java)
        `when`(client.accessSecretVersion(anyRequest()))
            .thenReturn(AccessSecretVersionResponse.getDefaultInstance())
            .thenReturn(response("secret", checksumOverride = 1))
            .thenReturn(response(ByteString.copyFrom(byteArrayOf(0xC3.toByte(), 0x28))))
            .thenReturn(response("""{"client-secret":{"nested":true}}"""))
        val registry = ClientSecretResolverRegistry(listOf(resolver(client)))

        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            registry.verify("gcp-sm:company-prod/kudos-auth-missing").status,
        )
        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            registry.verify("gcp-sm:company-prod/kudos-auth-checksum").status,
        )
        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            registry.verify("gcp-sm:company-prod/kudos-auth-utf8").status,
        )
        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            registry.verify("gcp-sm:company-prod/kudos-auth-json#client-secret").status,
        )
    }

    @Test
    fun apiErrorsAndMissingClientCollapseToSafeStatus() {
        val client = mock(SecretManagerServiceClient::class.java)
        `when`(client.accessSecretVersion(anyRequest())).thenThrow(
            IllegalStateException("resource and API diagnostic must not escape")
        )
        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            ClientSecretResolverRegistry(listOf(resolver(client)))
                .verify("gcp-sm:company-prod/kudos-auth-failure").status,
        )
        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            ClientSecretResolverRegistry(listOf(resolver()))
                .verify("gcp-sm:company-prod/kudos-auth-no-client").status,
        )
    }

    @Test
    fun registryInvalidationReadsLatestAgainAfterRotation() {
        val client = mock(SecretManagerServiceClient::class.java)
        `when`(client.accessSecretVersion(anyRequest()))
            .thenReturn(response("old-secret"))
            .thenReturn(response("new-secret"))
        val cache = ExternalLoginProperties().apply { secretCacheTtlSeconds = 60 }
        val registry = ClientSecretResolverRegistry(listOf(resolver(client)), cache)

        assertEquals("old-secret", registry.resolve("gcp-sm:company-prod/kudos-auth-google"))
        assertEquals("old-secret", registry.resolve("gcp-sm:company-prod/kudos-auth-google"))
        registry.invalidate("gcp-sm:company-prod/kudos-auth-google")
        assertEquals("new-secret", registry.resolve("gcp-sm:company-prod/kudos-auth-google"))
        verify(client, times(2)).accessSecretVersion(anyRequest())
    }

    @Test
    fun serverMayPinAPositiveNumericVersion() {
        val client = mock(SecretManagerServiceClient::class.java)
        `when`(client.accessSecretVersion(anyRequest())).thenReturn(response("pinned-secret"))
        val properties = properties().apply { version = "42" }

        assertEquals(
            "pinned-secret",
            resolver(client, properties).resolve("gcp-sm:company-prod/kudos-auth-google"),
        )
        val captor = ArgumentCaptor.forClass(AccessSecretVersionRequest::class.java)
        verify(client).accessSecretVersion(
            captor.capture() ?: AccessSecretVersionRequest.getDefaultInstance()
        )
        assertEquals(
            "projects/company-prod/secrets/kudos-auth-google/versions/42",
            captor.value.name,
        )
    }

    @Test
    fun invalidServerPolicyAndVersionAreResolverErrors() {
        val client = mock(SecretManagerServiceClient::class.java)
        val invalidProject = properties().apply { allowedProjectIds = listOf("projects/company-prod") }
        assertStatus(ClientSecretResolutionStatus.RESOLVER_ERROR) {
            resolver(client, invalidProject).resolve("gcp-sm:company-prod/kudos-auth-google")
        }
        val invalidPrefix = properties().apply { allowedSecretIdPrefixes = listOf("bad/prefix") }
        assertStatus(ClientSecretResolutionStatus.RESOLVER_ERROR) {
            resolver(client, invalidPrefix).resolve("gcp-sm:company-prod/kudos-auth-google")
        }
        val invalidVersion = properties().apply { version = "previous" }
        assertStatus(ClientSecretResolutionStatus.RESOLVER_ERROR) {
            resolver(client, invalidVersion).resolve("gcp-sm:company-prod/kudos-auth-google")
        }
    }

    private fun resolver(
        client: SecretManagerServiceClient? = null,
        properties: GoogleSecretManagerClientSecretProperties = properties(),
    ): GoogleSecretManagerClientSecretResolver {
        @Suppress("UNCHECKED_CAST")
        val provider = mock(ObjectProvider::class.java) as ObjectProvider<SecretManagerServiceClient>
        `when`(provider.getIfAvailable()).thenReturn(client)
        return GoogleSecretManagerClientSecretResolver(provider, properties)
    }

    private fun properties() = GoogleSecretManagerClientSecretProperties().apply {
        allowedProjectIds = listOf("company-prod")
    }

    private fun response(value: String, checksumOverride: Long? = null): AccessSecretVersionResponse =
        response(ByteString.copyFromUtf8(value), checksumOverride)

    private fun response(data: ByteString, checksumOverride: Long? = null): AccessSecretVersionResponse {
        val checksum = checksumOverride ?: CRC32C().apply { update(data.toByteArray()) }.value
        val payload = SecretPayload.newBuilder().setData(data).setDataCrc32C(checksum).build()
        return AccessSecretVersionResponse.newBuilder().setPayload(payload).build()
    }

    private fun anyRequest(): AccessSecretVersionRequest =
        org.mockito.ArgumentMatchers.any(AccessSecretVersionRequest::class.java)
            ?: AccessSecretVersionRequest.getDefaultInstance()

    private fun assertStatus(status: ClientSecretResolutionStatus, block: () -> Unit) {
        val error = assertFailsWith<ClientSecretResolutionException>(block = block)
        assertEquals(status, error.status)
    }
}
