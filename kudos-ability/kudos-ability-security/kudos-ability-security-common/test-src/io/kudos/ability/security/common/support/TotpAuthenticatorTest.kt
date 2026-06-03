package io.kudos.ability.security.common.support

import org.apache.commons.codec.binary.Base32
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for [TotpAuthenticator] covering the three contract surfaces: random key generation,
 * code minting at a known time, and verification within / outside the drift window.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class TotpAuthenticatorTest {

    @Test
    fun generateKey_returnsDecodableBase32_ofExpectedLength() {
        val authenticator = TotpAuthenticator()
        val secret = authenticator.generateKey()
        // 10 raw bytes encode to a 16-char Base32 string (10 bytes is a multiple of the Base32
        // 5-bit grouping, so there's no padding).
        assertEquals(16, secret.length)
        val decoded = Base32().decode(secret)
        assertEquals(10, decoded.size)
    }

    @Test
    fun generateKey_isNonDeterministic_acrossInstances() {
        // The key improvement over the inherited kudos-base impl: no fixed-seed SecureRandom,
        // so two instances independently produce different secrets. This is the property that
        // makes the secret a secret.
        val a = TotpAuthenticator().generateKey()
        val b = TotpAuthenticator().generateKey()
        assertNotEquals(a, b)
    }

    @Test
    fun generateCode_producesSixDigitsForAFixedClock() {
        val secret = TotpAuthenticator().generateKey()
        val clock = fixedClock(seconds = 1_700_000_000L)
        val authenticator = TotpAuthenticator(clock = clock)

        val code = authenticator.generateCode(secret)

        assertEquals(6, code.length, "TOTP code should be 6 digits including any leading zeros")
        assertTrue(code.all { it.isDigit() }, "TOTP code should be all digits, got `$code`")
    }

    @Test
    fun generateCode_isDeterministicForSameSecretAndClock() {
        val secret = TotpAuthenticator().generateKey()
        val clock = fixedClock(seconds = 1_700_000_000L)
        val a = TotpAuthenticator(clock = clock).generateCode(secret)
        val b = TotpAuthenticator(clock = clock).generateCode(secret)
        assertEquals(a, b)
    }

    @Test
    fun verify_acceptsCodeGeneratedAtSameInstant() {
        val secret = TotpAuthenticator().generateKey()
        val clock = fixedClock(seconds = 1_700_000_000L)
        val authenticator = TotpAuthenticator(clock = clock)

        val code = authenticator.generateCode(secret).toInt()

        assertTrue(authenticator.verify(secret, code))
    }

    @Test
    fun verify_rejectsCodeFromOutsideTheWindow() {
        val secret = TotpAuthenticator().generateKey()
        val mintClock = fixedClock(seconds = 1_700_000_000L)
        // windowSize=1 -> ±30s drift. A code minted 5 windows away (150s in the past) must not
        // verify against the current window.
        val verifyClock = fixedClock(seconds = 1_700_000_000L + 150L)

        val staleCode = TotpAuthenticator(clock = mintClock).generateCode(secret).toInt()
        val authenticator = TotpAuthenticator(windowSize = 1, clock = verifyClock)

        assertFalse(authenticator.verify(secret, staleCode))
    }

    @Test
    fun verify_acceptsCodeFromAdjacentWindow_whenWindowSizeAllowsIt() {
        val secret = TotpAuthenticator().generateKey()
        val mintClock = fixedClock(seconds = 1_700_000_000L)
        // Code minted ~30s in the past; outside windowSize=0 (strict), inside windowSize=1.
        val verifyClock = fixedClock(seconds = 1_700_000_000L + 30L)
        val code = TotpAuthenticator(clock = mintClock).generateCode(secret).toInt()

        assertTrue(TotpAuthenticator(windowSize = 1, clock = verifyClock).verify(secret, code))
    }

    @Test
    fun verify_rejectsWrongCode() {
        val secret = TotpAuthenticator().generateKey()
        val clock = fixedClock(seconds = 1_700_000_000L)
        val authenticator = TotpAuthenticator(clock = clock)

        val correct = authenticator.generateCode(secret).toInt()
        // Off-by-one in the visible-digit space: different code, valid format.
        val wrong = (correct + 1) % 1_000_000

        assertFalse(authenticator.verify(secret, wrong))
    }

    private fun fixedClock(seconds: Long): Clock =
        Clock.fixed(Instant.ofEpochSecond(seconds), ZoneOffset.UTC)
}
