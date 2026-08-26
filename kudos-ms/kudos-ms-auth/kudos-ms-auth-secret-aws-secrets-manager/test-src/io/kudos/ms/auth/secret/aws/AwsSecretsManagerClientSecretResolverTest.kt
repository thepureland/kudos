package io.kudos.ms.auth.secret.aws

import io.kudos.ms.auth.provider.oauth2.init.ExternalLoginProperties
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolutionException
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolutionStatus
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolverRegistry
import io.kudos.ms.auth.secret.aws.init.AwsSecretsManagerClientSecretProperties
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.ObjectProvider
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

internal class AwsSecretsManagerClientSecretResolverTest {

    @Test
    fun readsRawSecretStringAtServerOwnedRotationStage() {
        val client = mock(SecretsManagerClient::class.java)
        `when`(client.getSecretValue(anyRequest())).thenReturn(response("google-secret"))
        val resolver = resolver(client)

        assertEquals("google-secret", resolver.resolve("aws-sm:kudos/auth/tenant-a/google"))

        val captor = ArgumentCaptor.forClass(GetSecretValueRequest::class.java)
        verify(client).getSecretValue(
            captor.capture() ?: GetSecretValueRequest.builder().secretId("fallback").build()
        )
        assertEquals("kudos/auth/tenant-a/google", captor.value.secretId())
        assertEquals("AWSCURRENT", captor.value.versionStage())
        assertEquals(null, captor.value.versionId())
    }

    @Test
    fun readsExactTopLevelJsonFieldWithoutExpressionEvaluation() {
        val client = mock(SecretsManagerClient::class.java)
        `when`(client.getSecretValue(anyRequest())).thenReturn(
            response("""{"client-secret":"line-secret","nested":{"client-secret":"wrong"}}""")
        )

        assertEquals(
            "line-secret",
            resolver(client).resolve("aws-sm:kudos/auth/tenant-a/line#client-secret"),
        )
    }

    @Test
    fun absentFieldOrBlankSecretIsNotFound() {
        val client = mock(SecretsManagerClient::class.java)
        `when`(client.getSecretValue(anyRequest()))
            .thenReturn(response("""{"other":"value"}"""))
            .thenReturn(response("   "))
        val resolver = resolver(client)

        assertNull(resolver.resolve("aws-sm:kudos/auth/provider#client-secret"))
        assertNull(resolver.resolve("aws-sm:kudos/auth/provider"))
    }

    @Test
    fun pathTraversalEncodingAndVersionSelectorsAreRejectedBeforeSdkCall() {
        val client = mock(SecretsManagerClient::class.java)
        val resolver = resolver(client)
        listOf(
            "aws-sm:/kudos/auth/google",
            "aws-sm:kudos/auth/../google",
            "aws-sm:kudos/auth/%2e%2e/google",
            "aws-sm:kudos\\auth\\google",
            "aws-sm:kudos/auth/google?versionStage=AWSPREVIOUS",
            "aws-sm:kudos/auth/google#",
            "aws-sm:kudos/auth/google#field#other",
        ).forEach { reference ->
            assertStatus(ClientSecretResolutionStatus.INVALID_REFERENCE) { resolver.resolve(reference) }
        }
        verify(client, never()).getSecretValue(anyRequest())
    }

    @Test
    fun namePolicyUsesSegmentBoundariesAndEmptyPrefixesDenyAll() {
        assertStatus(ClientSecretResolutionStatus.POLICY_DENIED) {
            resolver().resolve("aws-sm:kudos/auth2/google")
        }
        val properties = properties().apply { allowedSecretIdPrefixes = emptyList() }
        assertStatus(ClientSecretResolutionStatus.POLICY_DENIED) {
            resolver(properties = properties).resolve("aws-sm:kudos/auth/google")
        }
    }

    @Test
    fun arnsRequireExplicitFlagAndAccountRegionNamespacePrefix() {
        val arn = "arn:aws:secretsmanager:ap-northeast-1:123456789012:secret:kudos/auth/google-AbCdEf"
        assertStatus(ClientSecretResolutionStatus.POLICY_DENIED) {
            resolver().resolve("aws-sm:$arn")
        }

        val client = mock(SecretsManagerClient::class.java)
        `when`(client.getSecretValue(anyRequest())).thenReturn(response("arn-secret"))
        val properties = properties().apply {
            allowArns = true
            allowedArnPrefixes = listOf(
                "arn:aws:secretsmanager:ap-northeast-1:123456789012:secret:kudos/auth/"
            )
        }
        assertEquals("arn-secret", resolver(client, properties).resolve("aws-sm:$arn"))

        assertStatus(ClientSecretResolutionStatus.POLICY_DENIED) {
            resolver(client, properties).resolve(
                "aws-sm:arn:aws:secretsmanager:us-east-1:123456789012:secret:kudos/auth/google-AbCdEf"
            )
        }

        properties.allowedArnPrefixes = listOf(
            "arn:aws:secretsmanager:ap-northeast-1:123456789012:secret:kudos/auth"
        )
        assertStatus(ClientSecretResolutionStatus.POLICY_DENIED) {
            resolver(client, properties).resolve("aws-sm:$arn")
        }
    }

    @Test
    fun sdkErrorsBinaryValuesAndInvalidJsonCollapseToSafeResolverError() {
        val failed = mock(SecretsManagerClient::class.java)
        `when`(failed.getSecretValue(anyRequest())).thenThrow(
            IllegalStateException("request id and backend details must not escape")
        )
        val failedRegistry = ClientSecretResolverRegistry(listOf(resolver(failed)))
        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            failedRegistry.verify("aws-sm:kudos/auth/failure").status,
        )

        val invalid = mock(SecretsManagerClient::class.java)
        `when`(invalid.getSecretValue(anyRequest()))
            .thenReturn(GetSecretValueResponse.builder().build())
            .thenReturn(response("not-json"))
            .thenReturn(response("""{"client-secret":{"nested":true}}"""))
        val invalidRegistry = ClientSecretResolverRegistry(listOf(resolver(invalid)))
        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            invalidRegistry.verify("aws-sm:kudos/auth/binary").status,
        )
        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            invalidRegistry.verify("aws-sm:kudos/auth/json#client-secret").status,
        )
        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            invalidRegistry.verify("aws-sm:kudos/auth/type#client-secret").status,
        )
    }

    @Test
    fun missingClientFailsClosedWithoutBreakingApplicationStartup() {
        val registry = ClientSecretResolverRegistry(listOf(resolver()))
        assertEquals(
            ClientSecretResolutionStatus.RESOLVER_ERROR,
            registry.verify("aws-sm:kudos/auth/google").status,
        )
    }

    @Test
    fun registryInvalidationLoadsAwscurrentAgainAfterRotation() {
        val client = mock(SecretsManagerClient::class.java)
        `when`(client.getSecretValue(anyRequest()))
            .thenReturn(response("old-secret"))
            .thenReturn(response("new-secret"))
        val cache = ExternalLoginProperties().apply { secretCacheTtlSeconds = 60 }
        val registry = ClientSecretResolverRegistry(listOf(resolver(client)), cache)

        assertEquals("old-secret", registry.resolve("aws-sm:kudos/auth/google"))
        assertEquals("old-secret", registry.resolve("aws-sm:kudos/auth/google"))
        registry.invalidate("aws-sm:kudos/auth/google")
        assertEquals("new-secret", registry.resolve("aws-sm:kudos/auth/google"))
        verify(client, times(2)).getSecretValue(anyRequest())
    }

    @Test
    fun invalidServerStageAndPrefixConfigurationAreResolverErrors() {
        val invalidStage = properties().apply { versionStage = "AWSCURRENT?version=old" }
        val client = mock(SecretsManagerClient::class.java)
        assertStatus(ClientSecretResolutionStatus.RESOLVER_ERROR) {
            resolver(client, invalidStage).resolve("aws-sm:kudos/auth/google")
        }
        val invalidPrefix = properties().apply { allowedSecretIdPrefixes = listOf("../auth") }
        assertStatus(ClientSecretResolutionStatus.RESOLVER_ERROR) {
            resolver(client, invalidPrefix).resolve("aws-sm:kudos/auth/google")
        }
        val invalidArnPrefix = properties().apply {
            allowArns = true
            allowedArnPrefixes = listOf("arn:aws:iam::123456789012:role/not-secrets-manager")
        }
        assertStatus(ClientSecretResolutionStatus.RESOLVER_ERROR) {
            resolver(client, invalidArnPrefix).resolve(
                "aws-sm:arn:aws:secretsmanager:ap-northeast-1:123456789012:secret:kudos/auth/google-AbCdEf"
            )
        }
    }

    private fun resolver(
        client: SecretsManagerClient? = null,
        properties: AwsSecretsManagerClientSecretProperties = properties(),
    ): AwsSecretsManagerClientSecretResolver {
        @Suppress("UNCHECKED_CAST")
        val provider = mock(ObjectProvider::class.java) as ObjectProvider<SecretsManagerClient>
        `when`(provider.getIfAvailable()).thenReturn(client)
        return AwsSecretsManagerClientSecretResolver(provider, properties)
    }

    private fun properties() = AwsSecretsManagerClientSecretProperties()

    private fun response(value: String) = GetSecretValueResponse.builder().secretString(value).build()

    private fun anyRequest(): GetSecretValueRequest =
        org.mockito.ArgumentMatchers.any(GetSecretValueRequest::class.java)
            ?: GetSecretValueRequest.builder().secretId("fallback").build()

    private fun assertStatus(status: ClientSecretResolutionStatus, block: () -> Unit) {
        val error = assertFailsWith<ClientSecretResolutionException>(block = block)
        assertEquals(status, error.status)
    }
}
