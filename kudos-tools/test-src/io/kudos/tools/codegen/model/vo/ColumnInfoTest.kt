package io.kudos.tools.codegen.model.vo

import kotlin.test.*

/**
 * test for ColumnInfo
 *
 * Covers every getter/setter/property accessor of the JavaFX-property-backed value object, plus the two
 * derived accessors:
 * - getComment(): custom comment wins when non-blank, otherwise falls back to the original comment, including
 *   null/empty/blank/Unicode boundaries;
 * - getColumn(): alias of getName().
 *
 * These are plain JavaFX beans (no Node/Stage), so they run without booting the FX toolkit.
 *
 * @author K
 * @since 1.0.0
 */
internal class ColumnInfoTest {

    @Test
    fun nameAndColumnAreAliases() {
        val info = ColumnInfo()
        assertNull(info.getName())
        assertNull(info.getColumn())

        info.setName("user_name")
        assertEquals("user_name", info.getName())
        assertEquals("user_name", info.getColumn(), "getColumn must alias getName")

        info.setName(null)
        assertNull(info.getName())
        assertNull(info.getColumn())
    }

    @Test
    fun booleanItemsDefaultFalseAndRoundTrip() {
        val info = ColumnInfo()
        // defaults
        assertFalse(info.getSearchItem())
        assertFalse(info.getListItem())
        assertFalse(info.getEditItem())
        assertFalse(info.getDetailItem())
        assertFalse(info.getCacheItem())

        info.setSearchItem(true)
        info.setListItem(true)
        info.setEditItem(true)
        info.setDetailItem(true)
        info.setCacheItem(true)

        assertTrue(info.getSearchItem())
        assertTrue(info.getListItem())
        assertTrue(info.getEditItem())
        assertTrue(info.getDetailItem())
        assertTrue(info.getCacheItem())

        // property accessors reflect the same backing value and are stable identities
        assertTrue(info.searchItemProperty().get())
        assertTrue(info.listItemProperty().get())
        assertTrue(info.editItemProperty().get())
        assertTrue(info.detailItemProperty().get())
        assertTrue(info.cacheItemProperty().get())
        assertSame(info.searchItemProperty(), info.searchItemProperty())
    }

    @Test
    fun propertyWritesAreVisibleThroughGetters() {
        val info = ColumnInfo()
        info.searchItemProperty().set(true)
        info.detailItemProperty().set(true)
        assertTrue(info.getSearchItem())
        assertTrue(info.getDetailItem())
        assertFalse(info.getListItem())
    }

    @Test
    fun customCommentRoundTrips() {
        val info = ColumnInfo()
        assertNull(info.getCustomComment())
        info.setCustomComment("自定义注释")
        assertEquals("自定义注释", info.getCustomComment())
        assertEquals("自定义注释", info.customCommentProperty().get())
    }

    @Test
    fun origCommentRoundTrips() {
        val info = ColumnInfo()
        assertNull(info.getOrigComment())
        info.setOrigComment("orig")
        assertEquals("orig", info.getOrigComment())
        info.setOrigComment(null)
        assertNull(info.getOrigComment())
    }

    @Test
    fun getCommentPrefersNonBlankCustomComment() {
        val info = ColumnInfo()
        info.setOrigComment("original")
        info.setCustomComment("custom")
        assertEquals("custom", info.getComment())
    }

    @Test
    fun getCommentFallsBackToOrigWhenCustomNull() {
        val info = ColumnInfo()
        info.setOrigComment("original")
        assertEquals("original", info.getComment(), "null custom comment must fall back to orig")
    }

    @Test
    fun getCommentFallsBackToOrigWhenCustomEmpty() {
        val info = ColumnInfo()
        info.setOrigComment("original")
        info.setCustomComment("")
        assertEquals("original", info.getComment(), "empty custom comment must fall back to orig")
    }

    @Test
    fun getCommentFallsBackToOrigWhenCustomBlank() {
        val info = ColumnInfo()
        info.setOrigComment("original")
        info.setCustomComment("   ")
        assertEquals("original", info.getComment(), "blank custom comment must fall back to orig")
    }

    @Test
    fun getCommentReturnsNullWhenBothEmpty() {
        val info = ColumnInfo()
        assertNull(info.getComment(), "both null -> null")

        info.setCustomComment("   ")
        info.setOrigComment(null)
        assertNull(info.getComment(), "blank custom + null orig -> null (orig fallback)")
    }

    @Test
    fun getCommentSupportsUnicodeCustom() {
        val info = ColumnInfo()
        info.setOrigComment("orig")
        info.setCustomComment("名前😀")
        assertEquals("名前😀", info.getComment())
    }
}
