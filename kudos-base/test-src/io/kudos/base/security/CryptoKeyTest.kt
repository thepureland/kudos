package io.kudos.base.security

import kotlin.test.*

internal class CryptoKeyTest {
    private var originalKey = ""
    @BeforeTest fun saveKey() { originalKey = CryptoKey.KEY_DEFAULT }
    @AfterTest fun restoreKey() { CryptoKey.KEY_DEFAULT = originalKey }

    @Test fun noImplicitDefaultOrBlankKey() {
        assertFailsWith<IllegalStateException> { CryptoKey.requireKey(null) }
        assertFailsWith<IllegalStateException> { CryptoKey.requireKey(" ") }
        assertFailsWith<IllegalArgumentException> { CryptoKey.requireKey("io．Kudos．base.security ") }
        assertFailsWith<IllegalArgumentException> { CryptoKey.KEY_DEFAULT = "short" }
    }

    @Test fun wrongDeploymentKeyCannotTurnAnEncryptedSecretIntoAnEmptyCredential() {
        val encrypted = CryptoKit.aesEncrypt("totp-secret")
        CryptoKey.configureDefaultKey("different-deployment-key-0123456789-abcdef")
        assertFails { CryptoKit.aesDecrypt(encrypted) }
        assertFails { CryptoKit.aesDecrypt("┼zz") }
    }

    @Test fun explicitKeyIsUsedForEncryption() {
        val key = "unit-test-only-key-0123456789-abcdefgh"
        CryptoKey.configureDefaultKey(key)
        assertEquals(key, CryptoKey.KEY_DEFAULT)
        val ciphertext = CryptoKit.aesEncrypt("test-secret")
        assertEquals("test-secret", CryptoKit.aesDecrypt(ciphertext))
        assertEquals("test-secret", CryptoKit.aesDecrypt(ciphertext.removePrefix("┼"), key))
    }
}
