package io.kudos.base.security

import kotlin.test.*

/**
 * test for PasswordKit
 *
 * @author K
 * @since 1.0.0
 */
internal class PasswordKitTest {

    @Test
    fun hashProducesValidBcryptAndVerifies() {
        val plain = "P@ssw0rd!"
        val hash = PasswordKit.hash(plain)

        // BCrypt output is a 60-char self-describing string
        assertEquals(60, hash.length)
        assertTrue(PasswordKit.looksLikeBcryptHash(hash))

        // round-trip verification
        assertTrue(PasswordKit.matches(plain, hash))
        assertFalse(PasswordKit.matches("wrong", hash))
    }

    @Test
    fun hashIsSaltedSoEachCallDiffers() {
        val plain = "samePassword"
        val h1 = PasswordKit.hash(plain)
        val h2 = PasswordKit.hash(plain)

        // random salt => different hashes, but both verify
        assertNotEquals(h1, h2)
        assertTrue(PasswordKit.matches(plain, h1))
        assertTrue(PasswordKit.matches(plain, h2))
    }

    @Test
    fun emptyPasswordIsRejected() {
        assertFailsWith<IllegalArgumentException> { PasswordKit.hash("") }
        assertFailsWith<IllegalArgumentException> { PasswordKit.hash("", 4) }
    }

    @Test
    fun hashWithExplicitStrengthVerifies() {
        val plain = "tune-cost"
        // use a low cost to keep the test fast
        val hash = PasswordKit.hash(plain, 4)
        assertEquals(60, hash.length)
        // cost factor is encoded in the hash prefix: $2a$04$
        assertTrue(hash.startsWith("\$2a\$04\$"))
        // verification works regardless of the encoder's default cost
        assertTrue(PasswordKit.matches(plain, hash))
    }

    @Test
    fun matchesIsNullAndBlankSafe() {
        assertFalse(PasswordKit.matches("whatever", null))
        assertFalse(PasswordKit.matches("whatever", ""))
    }

    @Test
    fun matchesSwallowsMalformedHash() {
        // not a BCrypt string; must return false instead of throwing
        assertFalse(PasswordKit.matches("whatever", "not-a-bcrypt-hash"))
        assertFalse(PasswordKit.matches("whatever", "\$2a\$10\$too-short"))
    }

    @Test
    fun looksLikeBcryptHashDistinguishesLegacyData() {
        assertTrue(PasswordKit.looksLikeBcryptHash("\$2a\$10\$" + "x".repeat(53)))
        assertTrue(PasswordKit.looksLikeBcryptHash("\$2b\$12\$" + "y".repeat(53)))
        assertTrue(PasswordKit.looksLikeBcryptHash("\$2y\$08\$" + "z".repeat(53)))

        // wrong length / prefix / null => not bcrypt
        assertFalse(PasswordKit.looksLikeBcryptHash(null))
        assertFalse(PasswordKit.looksLikeBcryptHash(""))
        assertFalse(PasswordKit.looksLikeBcryptHash("plain-text-password"))
        assertFalse(PasswordKit.looksLikeBcryptHash("\$2a\$10\$" + "x".repeat(52))) // 59 chars
        assertFalse(PasswordKit.looksLikeBcryptHash("\$1a\$10\$" + "x".repeat(53))) // bad version
    }
}
