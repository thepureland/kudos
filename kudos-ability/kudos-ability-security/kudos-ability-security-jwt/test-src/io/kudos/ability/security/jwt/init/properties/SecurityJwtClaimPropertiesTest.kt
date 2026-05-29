package io.kudos.ability.security.jwt.init.properties

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [SecurityJwtClaimProperties] — specifically the string-parsing rules for the
 * `nbf` / `iat` / `jti` fields.
 *
 * The parsing surface is funky on purpose (legacy soul yml compatibility); the tests lock down
 * the corner cases that those magic strings produce so future refactors don't silently change
 * behavior on existing deployments.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SecurityJwtClaimPropertiesTest {

    @Test
    fun resolveExpireAt_returnsNullWhenExpIsUnset() {
        assertNull(SecurityJwtClaimProperties().resolveExpireAt())
    }

    @Test
    fun resolveExpireAt_returnsFutureInstant() {
        val props = SecurityJwtClaimProperties().apply { exp = 3600 }
        val before = Instant.now()
        val resolved = assertNotNull(props.resolveExpireAt())
        val after = Instant.now()
        assertTrue(
            resolved >= before.plusSeconds(3600) && resolved <= after.plusSeconds(3600),
            "exp must add exactly the configured seconds to current time",
        )
    }

    @Test
    fun resolveNotBefore_returnsNullWhenNbfIsUnset() {
        assertNull(SecurityJwtClaimProperties().resolveNotBefore())
    }

    @Test
    fun resolveNotBefore_nowKeyword_returnsInstantNow() {
        val props = SecurityJwtClaimProperties().apply { nbf = "now()" }
        val before = Instant.now()
        val resolved = assertNotNull(props.resolveNotBefore())
        val after = Instant.now()
        assertTrue(resolved in before..after, "now() must produce an instant within the call window")
    }

    @Test
    fun resolveNotBefore_positiveNumber_returnsFutureInstant() {
        val props = SecurityJwtClaimProperties().apply { nbf = "120" }
        val resolved = assertNotNull(props.resolveNotBefore())
        assertTrue(
            resolved >= Instant.now().plus(Duration.ofSeconds(120).minusSeconds(1)),
            "positive numeric nbf must offset forward by that many seconds",
        )
    }

    @Test
    fun resolveNotBefore_zeroOrNegativeNumber_returnsNull() {
        // The original soul rule was "offset > 0 → set, else → omit". Lock it in.
        assertNull(SecurityJwtClaimProperties().apply { nbf = "0" }.resolveNotBefore())
        assertNull(SecurityJwtClaimProperties().apply { nbf = "-5" }.resolveNotBefore())
    }

    @Test
    fun resolveNotBefore_unrecognizedString_returnsNull() {
        assertNull(SecurityJwtClaimProperties().apply { nbf = "tomorrow" }.resolveNotBefore())
    }

    @Test
    fun resolveIssuedAt_onlyRecognizesNow() {
        assertNull(SecurityJwtClaimProperties().resolveIssuedAt())
        assertNull(SecurityJwtClaimProperties().apply { iat = "anything-else" }.resolveIssuedAt())
        assertNotNull(SecurityJwtClaimProperties().apply { iat = "now()" }.resolveIssuedAt())
    }

    @Test
    fun resolveJwtId_uuidKeyword_producesFreshHyphenlessUuidEachCall() {
        val props = SecurityJwtClaimProperties().apply { jti = "uuid()" }
        val first = props.resolveJwtId()
        val second = props.resolveJwtId()
        assertEquals(32, first.length, "UUID without hyphens must be exactly 32 hex chars")
        assertTrue(first.matches(Regex("[0-9a-fA-F]{32}")))
        assertNotEquals(first, second, "each resolveJwtId() call must mint a fresh UUID")
    }

    @Test
    fun resolveJwtId_unrecognizedSetting_returnsEmptyString() {
        // Soul's original behavior — keep it. Empty string is fine because
        // JwtClaimsSet.Builder.id("") effectively omits the claim.
        assertEquals("", SecurityJwtClaimProperties().resolveJwtId())
        assertEquals("", SecurityJwtClaimProperties().apply { jti = "fixed-string" }.resolveJwtId())
    }
}
