package io.kudos.base.lang.collections

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.ListResourceBundle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * XMap extension function test cases.
 *
 * Covers: containsAll (empty/sub-empty/match/mismatch) / toMap (ResourceBundle) / invertMap /
 * verbosePrint & debugPrint (with null map, nested map, self-reference and ancestor cyclic references).
 *
 * (toArrOfArr is covered in XMapTest.)
 *
 * @author K
 * @since 1.0.0
 */
internal class XMapExtTest {

    @Test
    fun containsAll() {
        val map = mapOf("a" to 1, "b" to 2, "c" to 3)
        assertTrue(map.containsAll(mapOf("a" to 1)))
        assertTrue(map.containsAll(mapOf("a" to 1, "b" to 2)))
        // value mismatch
        assertFalse(map.containsAll(mapOf("a" to 9)))
        // key missing
        assertFalse(map.containsAll(mapOf("z" to 1)))
        // empty receiver -> false
        assertFalse(emptyMap<String, Int>().containsAll(mapOf("a" to 1)))
        // empty subMap -> false
        assertFalse(map.containsAll(emptyMap<String, Int>()))
    }

    @Test
    fun toMap() {
        val bundle = object : ListResourceBundle() {
            override fun getContents(): Array<Array<Any>> = arrayOf(
                arrayOf("k1", "v1"),
                arrayOf("k2", 42)
            )
        }
        val map = bundle.toMap()
        assertEquals("v1", map["k1"])
        assertEquals(42, map["k2"])
        assertEquals(2, map.size)
    }

    @Test
    fun invertMap() {
        val map = mapOf("a" to 1, "b" to 2)
        val inverted = map.invertMap()
        assertEquals("a", inverted[1])
        assertEquals("b", inverted[2])
        assertEquals(2, inverted.size)
    }

    @Test
    fun verbosePrint_flatAndNested() {
        val out = ByteArrayOutputStream()
        val map = linkedMapOf<Any?, Any?>(
            "k1" to "v1",
            "k2" to linkedMapOf<Any?, Any?>("nk" to "nv")
        )
        map.verbosePrint(PrintStream(out), "label")
        val text = out.toString()
        assertTrue(text.contains("label = "))
        assertTrue(text.contains("k1 => v1"))
        assertTrue(text.contains("nk => nv"))
    }

    @Test
    fun debugPrint_includesTypeInfo() {
        val out = ByteArrayOutputStream()
        val map = linkedMapOf<Any?, Any?>("num" to 7)
        map.debugPrint(PrintStream(out), null) // null label branch
        val text = out.toString()
        assertTrue(text.contains("num => 7"))
        // debug mode appends the type name
        assertTrue(text.contains("java.lang.Integer"))
    }

    @Test
    fun verbosePrint_selfReferenceCyclic() {
        val out = ByteArrayOutputStream()
        val map = linkedMapOf<Any?, Any?>("a" to "b")
        // create a self-reference -> "(this Map)"
        map["self"] = map
        map.verbosePrint(PrintStream(out), null)
        val text = out.toString()
        assertTrue(text.contains("(this Map)"), "Expected self-reference marker, got: $text")
    }

    @Test
    fun verbosePrint_ancestorReferenceCyclic() {
        val out = ByteArrayOutputStream()
        val root = linkedMapOf<Any?, Any?>()
        val child = linkedMapOf<Any?, Any?>()
        val grandChild = linkedMapOf<Any?, Any?>()
        root["child"] = child
        child["grandChild"] = grandChild
        // grandChild points back to child, which is an ancestor at lineage index 1 (not the root) ->
        // "(ancestor[1] Map)"
        grandChild["backToChild"] = child
        root.verbosePrint(PrintStream(out), null)
        val text = out.toString()
        assertTrue(text.contains("ancestor["), "Expected ancestor-reference marker, got: $text")
    }

    @Test
    fun verbosePrint_nullKeyWithNestedMap() {
        val out = ByteArrayOutputStream()
        // a null key whose value is a nested map exercises the `childKey ?: "null"` label branch
        val nested = linkedMapOf<Any?, Any?>("inner" to "x")
        val map = linkedMapOf<Any?, Any?>(null to nested)
        map.verbosePrint(PrintStream(out), null)
        val text = out.toString()
        assertTrue(text.contains("null = "))
        assertTrue(text.contains("inner => x"))
    }

    @Test
    fun verbosePrint_nullValue() {
        val out = ByteArrayOutputStream()
        val map = linkedMapOf<Any?, Any?>("k" to null)
        map.verbosePrint(PrintStream(out), null)
        assertTrue(out.toString().contains("k => null"))
    }

}
