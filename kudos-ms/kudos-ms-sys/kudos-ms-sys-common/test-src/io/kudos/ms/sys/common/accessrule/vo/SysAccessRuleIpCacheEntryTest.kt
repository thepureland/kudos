package io.kudos.ms.sys.common.accessrule.vo

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysAccessRuleIpCacheEntry
 *
 * Covers property accessors (including the accessRuleTypeDictCode default null for legacy entries),
 * IIpBigDecimalToStringSupport overrides and inherited string rendering, data-class contract,
 * and Java serialization round-trip.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAccessRuleIpCacheEntryTest {

    private fun entry(type: String = "ipv4") = SysAccessRuleIpCacheEntry(
        id = "ip-1",
        ipStart = BigDecimal("3232235521"),
        ipEnd = BigDecimal("3232235530"),
        ipTypeDictCode = type,
        expirationTime = LocalDateTime.of(2030, 1, 2, 3, 4, 5),
        accessRuleTypeDictCode = "3",
    )

    @Test
    fun properties() {
        val e = entry()
        assertEquals("ip-1", e.id)
        assertEquals(BigDecimal("3232235521"), e.ipStart)
        assertEquals(BigDecimal("3232235530"), e.ipEnd)
        assertEquals("ipv4", e.ipTypeDictCode)
        assertEquals(LocalDateTime.of(2030, 1, 2, 3, 4, 5), e.expirationTime)
        assertEquals("3", e.accessRuleTypeDictCode)
    }

    @Test
    fun legacyEntryDefaultsRuleTypeToNull() {
        val legacy = SysAccessRuleIpCacheEntry(
            id = "ip-2",
            ipStart = BigDecimal.ONE,
            ipEnd = BigDecimal.TEN,
            ipTypeDictCode = "ipv6",
            expirationTime = null,
        )
        assertNull(legacy.accessRuleTypeDictCode)
        assertNull(legacy.expirationTime)
    }

    @Test
    fun ipSupportOverrides() {
        val e = entry()
        assertEquals(BigDecimal("3232235521"), e.getIpStartBigDecimal())
        assertEquals(BigDecimal("3232235530"), e.getIpEndBigDecimal())
        assertEquals("ipv4", e.getIpTypeDictCodeStr())
        // inherited default methods render dotted IPv4
        assertEquals("192.168.0.1", e.getIpStartStr())
        assertEquals("192.168.0.10", e.getIpEndStr())
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "other"))
        assertTrue(a.toString().contains("ip-1"))
    }

    @Test
    fun serializationRoundTrip() {
        val original = entry()
        val bytes = ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(original) }
            bos.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
        assertEquals(original, restored)
    }

}
