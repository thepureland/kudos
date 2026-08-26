package io.kudos.ms.auth.provider.oauth2.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ExternalClaimResolverTest {

    @Test
    fun resolvesOrderedMapAndListPathsWithTypedValues() {
        val claims = mapOf<String, Any?>(
            "empty" to null,
            "profiles" to listOf(mapOf("contact" to mapOf("email" to " alice@example.com "))),
            "verified" to true,
        )

        assertEquals(
            "alice@example.com",
            ExternalClaimResolver.firstString(claims, listOf("empty", "profiles.0.contact.email")),
        )
        assertEquals("true", ExternalClaimResolver.firstString(claims, listOf("verified")))
        assertNull(ExternalClaimResolver.first(claims, listOf("profiles.4.contact", "missing")))
    }
}
