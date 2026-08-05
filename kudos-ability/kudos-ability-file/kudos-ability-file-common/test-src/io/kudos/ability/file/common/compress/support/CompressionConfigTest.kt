package io.kudos.ability.file.common.compress.support

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [CompressionConfig].
 *
 * Covers: default values, property mutation, and the quality setter range check
 * (boundary values 0.0/1.0 accepted, out-of-range and NaN rejected with message).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class CompressionConfigTest {

    @Test
    fun defaults() {
        val c = CompressionConfig()
        assertFalse(c.enabled)
        assertFalse(c.webp)
        assertEquals(0, c.width)
        assertEquals(0, c.height)
        assertEquals(1f, c.quality)
    }

    @Test
    fun propertiesAreMutable() {
        val c = CompressionConfig().apply {
            enabled = true
            webp = true
            width = 800
            height = 600
        }
        assertTrue(c.enabled)
        assertTrue(c.webp)
        assertEquals(800, c.width)
        assertEquals(600, c.height)
    }

    @Test
    fun quality_acceptsBoundaryValues() {
        val c = CompressionConfig()
        c.quality = 0.0f
        assertEquals(0.0f, c.quality)
        c.quality = 1.0f
        assertEquals(1.0f, c.quality)
        c.quality = 0.75f
        assertEquals(0.75f, c.quality)
    }

    @Test
    fun quality_rejectsOutOfRange() {
        val c = CompressionConfig()
        val e1 = assertFailsWith<IllegalArgumentException> { c.quality = -0.01f }
        assertTrue(e1.message!!.contains("Quality must be between 0.0 and 1.0"))
        assertFailsWith<IllegalArgumentException> { c.quality = 1.01f }
        // NaN is not inside any closed range
        assertFailsWith<IllegalArgumentException> { c.quality = Float.NaN }
        // failed assignments must not corrupt the previous value
        assertEquals(1f, c.quality)
    }

}
