package io.kudos.ms.sys.core.accessrule.cache

import io.kudos.context.support.Consts
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure unit test for [AccessRuleTenantKey]: normalization of the tenant dimension and composite key building.
 * No Spring context or database required.
 *
 * Covers null / blank / whitespace-only / trimming / unicode tenant ids and the composite key format.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AccessRuleTenantKeyTest {

    private val delimiter = Consts.CACHE_KEY_DEFAULT_DELIMITER

    @Test
    fun normalize_null_returnsEmpty() {
        assertEquals("", AccessRuleTenantKey.normalize(null))
    }

    @Test
    fun normalize_empty_returnsEmpty() {
        assertEquals("", AccessRuleTenantKey.normalize(""))
    }

    @Test
    fun normalize_blankWhitespace_returnsEmpty() {
        assertEquals("", AccessRuleTenantKey.normalize("   "))
        assertEquals("", AccessRuleTenantKey.normalize("\t\n "))
    }

    @Test
    fun normalize_trimsSurroundingWhitespace() {
        assertEquals("tenant-1", AccessRuleTenantKey.normalize("  tenant-1  "))
    }

    @Test
    fun normalize_keepsInnerContentAsIs() {
        // only surrounding whitespace is trimmed; inner content is preserved
        assertEquals("a b", AccessRuleTenantKey.normalize(" a b "))
    }

    @Test
    fun normalize_unicodeTenantId() {
        assertEquals("租户-甲", AccessRuleTenantKey.normalize("  租户-甲  "))
    }

    @Test
    fun compositeKey_withTenant() {
        assertEquals("sys-a${delimiter}tenant-1", AccessRuleTenantKey.compositeKey("sys-a", "tenant-1"))
    }

    @Test
    fun compositeKey_nullTenant_isPlatformLevel() {
        assertEquals("sys-a${delimiter}", AccessRuleTenantKey.compositeKey("sys-a", null))
    }

    @Test
    fun compositeKey_blankTenant_isPlatformLevel() {
        assertEquals("sys-a${delimiter}", AccessRuleTenantKey.compositeKey("sys-a", "   "))
    }

    @Test
    fun compositeKey_trimsTenant() {
        assertEquals("sys-a${delimiter}t1", AccessRuleTenantKey.compositeKey("sys-a", " t1 "))
    }
}
