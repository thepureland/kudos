package io.kudos.base.security

import kotlin.test.*

/**
 * CryptoKey测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class CryptoKeyTest {

    private var originalKey: String = ""

    @BeforeTest
    fun setup() {
        // 保存原始值
        originalKey = CryptoKey.KEY_DEFAULT
    }

    @AfterTest
    fun teardown() {
        // 恢复原始值
        CryptoKey.KEY_DEFAULT = originalKey
    }

    @Test
    fun testDefaultKey() {
        val defaultKey = CryptoKey.KEY_DEFAULT
        assertNotNull(defaultKey)
        assertEquals("io．Kudos．base.security ", defaultKey)
    }

    @Test
    fun testConfigureDefaultKeyRejectsBlank() {
        assertFailsWith<IllegalArgumentException> {
            CryptoKey.configureDefaultKey("   ")
        }
        assertFailsWith<IllegalArgumentException> {
            CryptoKey.configureDefaultKey("")
        }
    }

    @Test
    fun testConfigureDefaultKeySetsKey() {
        val k = "configured-at-startup-${System.nanoTime()}"
        CryptoKey.configureDefaultKey(k)
        assertEquals(k, CryptoKey.KEY_DEFAULT)
    }

    @Test
    fun testSetKey() {
        val newKey = "new-secret-key"
        
        CryptoKey.KEY_DEFAULT = newKey
        assertEquals(newKey, CryptoKey.KEY_DEFAULT)
    }

    @Test
    fun testKeyCanBeChanged() {
        val key1 = "key1"
        val key2 = "key2"
        
        CryptoKey.KEY_DEFAULT = key1
        assertEquals(key1, CryptoKey.KEY_DEFAULT)
        
        CryptoKey.KEY_DEFAULT = key2
        assertEquals(key2, CryptoKey.KEY_DEFAULT)
    }

    @Test
    fun testKeyWithSpecialCharacters() {
        val specialKey = "key-with-special-chars!@#$%^&*()"
        
        CryptoKey.KEY_DEFAULT = specialKey
        assertEquals(specialKey, CryptoKey.KEY_DEFAULT)
    }

    @Test
    fun testKeyWithUnicode() {
        val unicodeKey = "密钥-中文-🔐"
        
        CryptoKey.KEY_DEFAULT = unicodeKey
        assertEquals(unicodeKey, CryptoKey.KEY_DEFAULT)
    }

    @Test
    fun testKeyCanBeEmpty() {
        CryptoKey.KEY_DEFAULT = ""
        assertEquals("", CryptoKey.KEY_DEFAULT)
    }

    @Test
    fun testKeyCanBeVeryLong() {
        val longKey = "a".repeat(1000)
        
        CryptoKey.KEY_DEFAULT = longKey
        assertEquals(longKey, CryptoKey.KEY_DEFAULT)
    }
}
