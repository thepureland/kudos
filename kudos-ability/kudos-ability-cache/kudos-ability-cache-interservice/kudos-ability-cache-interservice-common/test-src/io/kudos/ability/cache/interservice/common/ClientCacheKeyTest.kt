package io.kudos.ability.cache.interservice.common

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * ClientCacheKey 單元測試。
 *
 * 覆蓋：
 *  - 全參構造的三個 body 分支：非空 body / 空 body / null body
 *  - 無參構造（所有字段為 null）
 *  - toString 的拼接格式（url::method::param）
 *  - Unicode body（String(ByteArray) 以 UTF-8 解碼）
 *  - 伴生對象常量值
 *  - JVM 原生序列化往返（Serializable 契約）
 *
 * @author K
 * @since 1.0.0
 */
internal class ClientCacheKeyTest {

    @Test
    fun toString_fullConstructor_nonEmptyBody() {
        val key = ClientCacheKey("/api/user", "POST", "id=1&name=a".toByteArray())
        assertEquals("/api/user::POST::id=1&name=a", key.toString())
    }

    @Test
    fun toString_emptyBody_paramStaysNull() {
        val key = ClientCacheKey("/api/user", "GET", ByteArray(0))
        assertEquals("/api/user::GET::null", key.toString())
    }

    @Test
    fun toString_nullBody_paramStaysNull() {
        val key = ClientCacheKey("/api/user", "GET", null)
        assertEquals("/api/user::GET::null", key.toString())
    }

    @Test
    fun toString_allArgsNull() {
        val key = ClientCacheKey(null, null, null)
        assertEquals("null::null::null", key.toString())
    }

    @Test
    fun toString_defaultConstructor_allFieldsNull() {
        assertEquals("null::null::null", ClientCacheKey().toString())
    }

    @Test
    fun toString_unicodeBody_decodedAsUtf8() {
        val body = "中文Кириллица😀".toByteArray(Charsets.UTF_8)
        val key = ClientCacheKey("/api/i18n", "POST", body)
        assertEquals("/api/i18n::POST::中文Кириллица😀", key.toString())
    }

    @Test
    fun toString_longBody_fullyPreserved() {
        val longParam = "x".repeat(10_000)
        val key = ClientCacheKey("/api/big", "PUT", longParam.toByteArray())
        assertEquals("/api/big::PUT::$longParam", key.toString())
    }

    @Test
    fun companionConstants_haveDocumentedValues() {
        assertEquals("FEIGN-CACHE", ClientCacheKey.FEIGN_CACHE_PREFIX)
        assertEquals("::", ClientCacheKey.FEIGN_CACHE_DELIMITER)
        assertEquals("cache-uid", ClientCacheKey.HEADER_KEY_CACHE_UID)
        assertEquals("cache-key", ClientCacheKey.HEADER_KEY_CACHE_KEY)
        assertEquals("cache-status", ClientCacheKey.HEADER_KEY_CACHE_STATUS)
        assertEquals("304", ClientCacheKey.STATUS_USE_CACHE)
        assertEquals("200", ClientCacheKey.STATUS_DO_CACHE)
    }

    @Test
    fun jvmSerialization_roundTrip_preservesToString() {
        val original = ClientCacheKey("/api/user", "POST", "p=1".toByteArray())

        val bytes = ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(original) }
            bos.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() } as ClientCacheKey

        assertEquals(original.toString(), restored.toString())
        assertEquals("/api/user::POST::p=1", restored.toString())
    }
}
