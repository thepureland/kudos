package io.kudos.base.bean.validation.teminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PropertyResolver测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class PropertyResolverTest {

    @Test
    fun testToPot() {
        assertEquals("name", PropertyResolver.toPot("name"))
        assertEquals("user.name", PropertyResolver.toPot("user_name"))
        assertEquals("user.name", PropertyResolver.toPot("user.name"))
        assertEquals("name", PropertyResolver.toPot($$"$name"))
        assertEquals("name[]", PropertyResolver.toPot($$$"$$name"))
    }

    @Test
    fun testToPotWithArray() {
        assertEquals("users[]", PropertyResolver.toPot($$$"$$users"))
        assertEquals("users[0]", PropertyResolver.toPot("users[0]"))
    }

    @Test
    fun testToPotQuote() {
        assertEquals("name", PropertyResolver.toPotQuote("name"))
        assertEquals("'user.name'", PropertyResolver.toPotQuote("user.name"))
        assertEquals("'users[]'", PropertyResolver.toPotQuote("users[]"))
        assertEquals("name", PropertyResolver.toPotQuote("name"))
    }

    @Test
    fun testToPotQuoteWithPrefix() {
        assertEquals("'prefix.name'", PropertyResolver.toPotQuote("name", "prefix"))
        assertEquals("name", PropertyResolver.toPotQuote("name", ""))
//        assertEquals("name", PropertyResolver.toPotQuote("", "prefix"))
    }

    @Test
    fun testToPotQuoteWithPrefixAndSpecialChars() {
        // 如果属性名已经包含下划线或点，不添加前缀
        assertEquals("'user_name'", PropertyResolver.toPotQuote("user_name", "prefix"))
        assertEquals("'user.name'", PropertyResolver.toPotQuote("user.name", "prefix"))
        
        // 如果属性名以$开头，不添加前缀
//        assertEquals($$"$name", PropertyResolver.toPotQuote($$"$name", "prefix"))
        
        // 如果属性名以'开头，不添加前缀
        assertEquals("'name'", PropertyResolver.toPotQuote("'name'", "prefix"))
        
//        // 如果属性名已经包含前缀，不添加
//        assertEquals("prefix.name", PropertyResolver.toPotQuote("prefix.name", "prefix"))
    }

    @Test
    fun testToUnderline() {
        assertEquals($$"$name", PropertyResolver.toUnderline("name"))
        assertEquals("user_name", PropertyResolver.toUnderline("user.name"))
        assertEquals("user_name", PropertyResolver.toUnderline("user_name"))
    }

    @Test
    fun testToUnderlineWithArray() {
        assertEquals($$$"$$users", PropertyResolver.toUnderline("users[0]"))
        assertEquals($$$"$$users", PropertyResolver.toUnderline("users[1]"))
        assertEquals("users[0].name", PropertyResolver.toUnderline("users[0].name"))
    }

    @Test
    fun testIsArrayProperty() {
        assertTrue(PropertyResolver.isArrayProperty("users[0]"))
        assertTrue(PropertyResolver.isArrayProperty("users[1]"))
        assertTrue(PropertyResolver.isArrayProperty("users[0].name"))
        assertFalse(PropertyResolver.isArrayProperty("users"))
        assertFalse(PropertyResolver.isArrayProperty("users[]"))
    }

    @Test
    fun testToPotQuoteWithBlank() {
        assertEquals("", PropertyResolver.toPotQuote(""))
        assertEquals("", PropertyResolver.toPotQuote("   "))
        assertEquals("", PropertyResolver.toPotQuote("", "prefix"))
    }
}
