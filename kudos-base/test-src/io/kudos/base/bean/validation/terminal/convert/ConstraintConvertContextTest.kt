package io.kudos.base.bean.validation.terminal.convert

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Test cases for [ConstraintConvertContext], covering both constructors and mutability of its fields.
 *
 * @author K
 * @since 1.0.0
 */
internal class ConstraintConvertContextTest {

    @Test
    fun threeArgConstructorLeavesOriginalPropertyNull() {
        val ctx = ConstraintConvertContext("name", "prefix", String::class)
        assertEquals("name", ctx.property)
        assertEquals("prefix", ctx.propertyPrefix)
        assertSame(String::class, ctx.beanClass)
        assertNull(ctx.originalProperty)
    }

    @Test
    fun fourArgConstructorSetsOriginalProperty() {
        val ctx = ConstraintConvertContext("orig", "name", "prefix", Int::class)
        assertEquals("orig", ctx.originalProperty)
        assertEquals("name", ctx.property)
        assertEquals("prefix", ctx.propertyPrefix)
        assertSame(Int::class, ctx.beanClass)
    }

    @Test
    fun fieldsAreMutable() {
        val ctx = ConstraintConvertContext("name", null, String::class)
        ctx.property = "renamed"
        ctx.propertyPrefix = "p"
        ctx.beanClass = Int::class
        ctx.originalProperty = "o"
        assertEquals("renamed", ctx.property)
        assertEquals("p", ctx.propertyPrefix)
        assertSame(Int::class, ctx.beanClass)
        assertEquals("o", ctx.originalProperty)
    }
}
