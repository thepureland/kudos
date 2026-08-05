package io.kudos.ms.sys.core.outline.cache

import io.kudos.context.support.Consts
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure unit test for [OutLineSystemTenantKey]: tenant dimension normalization and composite key building.
 * No Spring context or database required.
 *
 * Covers null / blank / whitespace-only / trimming / unicode tenant ids and the composite key format.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class OutLineSystemTenantKeyTest {

    private val delimiter = Consts.CACHE_KEY_DEFAULT_DELIMITER

    @Test
    fun normalize_null_returnsEmpty() {
        assertEquals("", OutLineSystemTenantKey.normalize(null))
    }

    @Test
    fun normalize_emptyAndBlank_returnsEmpty() {
        assertEquals("", OutLineSystemTenantKey.normalize(""))
        assertEquals("", OutLineSystemTenantKey.normalize("   "))
    }

    @Test
    fun normalize_trimsSurroundingWhitespace() {
        assertEquals("t-9", OutLineSystemTenantKey.normalize("  t-9  "))
    }

    @Test
    fun normalize_unicode() {
        assertEquals("租户9", OutLineSystemTenantKey.normalize(" 租户9 "))
    }

    @Test
    fun compositeKey_withTenant() {
        assertEquals("sys-out${delimiter}t-9", OutLineSystemTenantKey.compositeKey("sys-out", "t-9"))
    }

    @Test
    fun compositeKey_nullTenant_isPlatformLevel() {
        assertEquals("sys-out${delimiter}", OutLineSystemTenantKey.compositeKey("sys-out", null))
    }

    @Test
    fun compositeKey_blankTenant_isPlatformLevel() {
        assertEquals("sys-out${delimiter}", OutLineSystemTenantKey.compositeKey("sys-out", "  "))
    }
}
