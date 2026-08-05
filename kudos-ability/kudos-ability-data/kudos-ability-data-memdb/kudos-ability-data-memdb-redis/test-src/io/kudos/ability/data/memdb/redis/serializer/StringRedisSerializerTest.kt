package io.kudos.ability.data.memdb.redis.serializer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for the prefix-aware [StringRedisSerializer].
 *
 * Covers: prefix added on serialize, prefix stripped on deserialize, legacy (unprefixed) value
 * preservation, null bytes, Unicode prefix/key, key identical to the prefix, and round-trip.
 * Pure unit test, no Redis required.
 *
 * @author K
 * @since 1.0.0
 */
internal class StringRedisSerializerTest {

    private val serializer = StringRedisSerializer("tenant1")

    @Test
    fun serialize_prependsPrefix() {
        assertEquals("tenant1:myKey", String(serializer.serialize("myKey"), Charsets.UTF_8))
    }

    @Test
    fun serialize_emptyKey_yieldsBarePrefix() {
        assertEquals("tenant1:", String(serializer.serialize(""), Charsets.UTF_8))
    }

    @Test
    fun deserialize_stripsPrefix() {
        assertEquals("myKey", serializer.deserialize("tenant1:myKey".toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun deserialize_nullBytes_returnsNull() {
        assertNull(serializer.deserialize(null))
    }

    @Test
    fun deserialize_legacyUnprefixedValue_isPreserved() {
        assertEquals("legacyKey", serializer.deserialize("legacyKey".toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun deserialize_valueEqualToPrefix_yieldsEmptyString() {
        // removePrefix leaves "", which is shorter than the original -> treated as a stripped (empty) key
        assertEquals("", serializer.deserialize("tenant1:".toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun roundTrip_returnsOriginalKey() {
        val key = "session:user:42"
        assertEquals(key, serializer.deserialize(serializer.serialize(key)))
    }

    @Test
    fun unicode_prefixAndKey_roundTrip() {
        val unicodeSerializer = StringRedisSerializer("租戶甲")
        val key = "鍵-値-🔑"
        assertEquals("租戶甲:$key", String(unicodeSerializer.serialize(key), Charsets.UTF_8))
        assertEquals(key, unicodeSerializer.deserialize(unicodeSerializer.serialize(key)))
    }

    @Test
    fun nullPrefix_isRenderedLiterally() {
        // Constructor accepts a nullable prefix; "$prefix:" renders null as the literal "null:"
        val nullPrefixSerializer = StringRedisSerializer(null)
        assertEquals("null:k", String(nullPrefixSerializer.serialize("k"), Charsets.UTF_8))
        assertEquals("k", nullPrefixSerializer.deserialize("null:k".toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun longKey_roundTrip() {
        val key = "k".repeat(10_000)
        assertEquals(key, serializer.deserialize(serializer.serialize(key)))
    }
}
