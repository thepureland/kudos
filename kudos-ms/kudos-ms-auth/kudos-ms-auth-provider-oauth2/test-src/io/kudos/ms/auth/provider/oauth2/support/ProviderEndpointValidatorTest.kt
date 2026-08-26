package io.kudos.ms.auth.provider.oauth2.support

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class ProviderEndpointValidatorTest {

    @Test
    fun acceptsPublicHttpsEndpoint() {
        assertEquals(
            "https://accounts.google.com/o/oauth2/v2/auth",
            ProviderEndpointValidator.requireSafeHttps(
                "https://accounts.google.com/o/oauth2/v2/auth",
                "authorization_uri",
            )
        )
    }

    @Test
    fun rejectsHttpLocalhostAndPrivateIp() {
        assertFailsWith<IllegalArgumentException> {
            ProviderEndpointValidator.requireSafeHttps("http://example.com/auth", "authorization_uri")
        }
        assertFailsWith<IllegalArgumentException> {
            ProviderEndpointValidator.requireSafeHttps("https://localhost/auth", "authorization_uri")
        }
        assertFailsWith<IllegalArgumentException> {
            ProviderEndpointValidator.requireSafeHttps("https://127.0.0.1/auth", "authorization_uri")
        }
        assertFailsWith<IllegalArgumentException> {
            ProviderEndpointValidator.requireSafeHttps("https://192.168.1.10/auth", "authorization_uri")
        }
    }
}
