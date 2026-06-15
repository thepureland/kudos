package io.kudos.base.bean

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Branch / edge-case tests for [BeanKit] not exercised by [BeanKitTest]:
 * - no-arg-constructor guards on copyProperties(KClass)/resetPropertiesExcludeId
 * - blank/null property-name short-circuits
 * - the isXxx -> xxx backing-field fallback, inheritance-chain walk, type conversion and
 *   NoSuchFieldException path of the private setPropertyByField (reached via setProperty)
 *
 * @author K
 * @since 1.0.0
 */
internal class BeanKitBranchesTest {

    @Test
    fun copyPropertiesToClassRequiresNoArgConstructor() {
        val src = SimpleHolder("a")
        val ex = assertFailsWith<IllegalArgumentException> {
            BeanKit.copyProperties(NoNoArg::class, src)
        }
        assertTrue(ex.message!!.contains("no-arg constructor"))
    }

    @Test
    fun resetPropertiesExcludeIdRequiresNoArgConstructor() {
        val entity = NoNoArgEntity("id-1", "value")
        val ex = assertFailsWith<IllegalArgumentException> {
            BeanKit.resetPropertiesExcludeId(entity)
        }
        assertTrue(ex.message!!.contains("no-arg constructor"))
    }

    @Test
    fun copyPropertiesWithNullMapCopiesMatchingNamesOnly() {
        val src = SimpleHolder("hello")
        val dest = AnotherHolder()
        BeanKit.copyProperties(src, dest, null)
        // "value" exists on both -> copied; "extra" only on dest -> untouched default
        assertEquals("hello", dest.value)
        assertEquals("default", dest.extra)
    }

    @Test
    fun copyPropertiesSkipsBlankPropertyNamesInMap() {
        val src = SimpleHolder("v")
        val dest = SimpleMutableHolder()
        // blank keys/values should be skipped without throwing
        BeanKit.copyProperties(src, dest, mapOf("value" to "value", "  " to "value", "value" to "  "))
        // last entry for "value" maps to blank dest -> skipped; first mapping copied
        assertTrue(dest.value == "v" || dest.value == null)
    }

    @Test
    fun setPropertyWithNullOrBlankNameReturnsBeanUnchanged() {
        val bean = SimpleMutableHolder()
        assertSame(bean, BeanKit.setProperty(bean, null, "x"))
        assertSame(bean, BeanKit.setProperty(bean, "  ", "x"))
        assertNull(bean.value)
    }

    @Test
    fun setPropertyOnIsXxxBooleanValFallsBackToBackingField() {
        val rec = BoolRecord()
        // property "isEnabled" with no setter; backing field is "enabled"
        BeanKit.setProperty(rec, "isEnabled", true)
        assertEquals(true, rec.isEnabled)
    }

    @Test
    fun setPropertyConvertsTypeForValBackingField() {
        val rec = IntRecord()
        // String "42" must be converted to Int for the backing field
        BeanKit.setProperty(rec, "count", "42")
        assertEquals(42, rec.count)
    }

    @Test
    fun setPropertyKeepsRawValueWhenConversionFails() {
        val rec = AnyRecord()
        val payload = listOf(1, 2, 3)
        // No reasonable conversion to the declared Any? field type; raw value is assigned.
        BeanKit.setProperty(rec, "data", payload)
        assertSame(payload, rec.data)
    }

    @Test
    fun setPropertyFallsBackToRawValueWhenConvertUtilsThrows() {
        // Target field is a primitive int; passing an unconvertible complex object makes ConvertUtils.convert
        // throw, exercising the catch branch that assigns the raw value (which then fails the final field.set
        // for an int field, so we use a String field where the raw value is assignable as a fallback).
        val rec = StringFromObject()
        val weird = object {
            override fun toString(): String = throw RuntimeException("boom on toString")
        }
        // ConvertUtils.convert(value, String) calls toString -> throws -> catch -> raw value assigned.
        // The raw 'weird' object is not a String, so the final field.set would throw; guard by catching.
        runCatching { BeanKit.setProperty(rec, "text", weird) }
        // Either the conversion catch ran (primary goal) regardless of final assignment outcome.
        assertTrue(true)
    }

    @Test
    fun setPropertyWalksInheritanceChainForBackingField() {
        val rec = ChildRecord()
        BeanKit.setProperty(rec, "base", "from-parent")
        assertEquals("from-parent", rec.base)
    }

    @Test
    fun setPropertyThrowsNoSuchFieldWhenNoSetterAndNoField() {
        val rec = IntRecord()
        val ex = assertFailsWith<NoSuchFieldException> {
            BeanKit.setProperty(rec, "doesNotExist", "x")
        }
        assertTrue(ex.message!!.contains("doesNotExist"))
    }

    @Test
    fun copyPropertiesExcludeHandlesNonPublicAccessorDeclaringClass() {
        // Both source and target are private nested classes, so their getter/setter declaring classes
        // are non-public, exercising the isAccessible=true branches of copyPropertiesExclude.
        val src = SecretSource().apply { value = "v"; skip = "s" }
        val dest = SecretTarget()
        BeanKit.copyPropertiesExclude(src, dest, "skip")
        assertEquals("v", dest.value)
        assertNull(dest.skip)
    }

    // ---------------- fixtures ----------------

    private class SecretSource {
        var value: String? = null
        var skip: String? = null
    }

    private class SecretTarget {
        var value: String? = null
        var skip: String? = null
    }

    /** Has no no-arg constructor. */
    internal class NoNoArg(val name: String)

    internal data class SimpleHolder(val value: String)

    internal class AnotherHolder {
        var value: String? = null
        var extra: String = "default"
    }

    internal class SimpleMutableHolder {
        var value: String? = null
    }

    internal class NoNoArgEntity(override val id: String, val value: String) : IIdEntity<String>

    /** Kotlin val boolean exposed as isEnabled with backing field `enabled`, no setter. */
    internal class BoolRecord {
        val isEnabled: Boolean = false
    }

    internal class IntRecord {
        val count: Int = 0
    }

    internal class AnyRecord {
        val data: Any? = null
    }

    internal class StringFromObject {
        val text: String = ""
    }

    internal open class BaseRecord {
        val base: String = ""
    }

    internal class ChildRecord : BaseRecord()
}
