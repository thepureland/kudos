package io.kudos.ms.auth.provider.oauth2.secret

import io.kudos.ms.auth.provider.oauth2.init.ExternalLoginProperties
import org.springframework.mock.env.MockEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class EnvironmentClientSecretResolverTest {

    @Test
    fun resolvesOnlyPropertiesInTheAuthenticationNamespace() {
        val environment = MockEnvironment()
            .withProperty("kudos.ms.auth.external-secrets.google", "google-secret")
            .withProperty("spring.datasource.password", "database-secret")
        val resolver = EnvironmentClientSecretResolver(environment, ExternalLoginProperties())

        assertEquals(
            "google-secret",
            resolver.resolve("property:kudos.ms.auth.external-secrets.google"),
        )
        assertStatus(ClientSecretResolutionStatus.POLICY_DENIED) {
            resolver.resolve("property:spring.datasource.password")
        }
    }

    @Test
    fun customPrefixMayBeConfiguredAndAnEmptyAllowlistDeniesAll() {
        val properties = ExternalLoginProperties().apply {
            allowedSecretPropertyPrefixes = listOf("company.authentication.")
        }
        val resolver = EnvironmentClientSecretResolver(
            MockEnvironment().withProperty("company.authentication.line", "line-secret"),
            properties,
        )
        assertEquals("line-secret", resolver.resolve("property:company.authentication.line"))

        properties.allowedSecretPropertyPrefixes = emptyList()
        assertStatus(ClientSecretResolutionStatus.POLICY_DENIED) {
            resolver.resolve("property:company.authentication.line")
        }
    }

    @Test
    fun malformedNamesAreRejectedBeforeEnvironmentLookup() {
        val resolver = EnvironmentClientSecretResolver(MockEnvironment(), ExternalLoginProperties())

        assertStatus(ClientSecretResolutionStatus.INVALID_REFERENCE) {
            resolver.resolve("property:kudos.ms.auth.external-secrets.bad name")
        }
        assertStatus(ClientSecretResolutionStatus.INVALID_REFERENCE) {
            resolver.resolve("env:1INVALID")
        }
    }

    private fun assertStatus(status: ClientSecretResolutionStatus, block: () -> Unit) {
        val error = assertFailsWith<ClientSecretResolutionException>(block = block)
        assertEquals(status, error.status)
    }
}
