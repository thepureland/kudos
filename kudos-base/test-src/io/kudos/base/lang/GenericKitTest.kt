package io.kudos.base.lang

import io.kudos.base.lang.reflect.getMemberFunction
import io.kudos.base.lang.reflect.getMemberProperty
import io.kudos.base.support.ICallback
import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Test cases for GenericKit.
 *
 * @author K
 * @since 1.0.0
 */
internal class GenericKitTest {

    @Test
    fun getSuperClassGenricType() {
        // No parent class; take the generic argument of the first implemented interface
        assertEquals(Int::class, GenericKit.getSuperClassGenricClass(TestGeneric1::class, 0))
        assertEquals(Map::class, GenericKit.getSuperClassGenricClass(TestGeneric1::class, 1))

        // Has parent class (parameterized on the parent; the lookup recurses upward)
        assertEquals(Int::class, GenericKit.getSuperClassGenricClass(TestGeneric2::class, 0))
        assertEquals(Map::class, GenericKit.getSuperClassGenricClass(TestGeneric2::class, 1))

        // Has parent class (parameterized on the current class)
        assertEquals(String::class, GenericKit.getSuperClassGenricClass(TestGeneric3::class, 0))
        assertEquals(Double::class, GenericKit.getSuperClassGenricClass(TestGeneric3::class, 1))

        // Index out of bounds
        assertFailsWith<IllegalArgumentException> { GenericKit.getSuperClassGenricClass(TestGeneric3::class, 2) }
    }

    @Test
    fun getParameterTypeGeneric() {
        val function = TestGeneric3::class.getMemberFunction("f")

        // Parameter 0 is the TestGeneric2 object
        assertEquals(
            listOf(Nothing::class), GenericKit.getParameterTypeGenericClass(function, 0)
        )

        // Type that does not support generic arguments
        assertEquals(listOf(Nothing::class), GenericKit.getParameterTypeGenericClass(function, 1))

        // Has concrete generic arguments
        assertEquals(listOf(String::class, Boolean::class), GenericKit.getParameterTypeGenericClass(function, 2))

        // Declared as "*"
        assertEquals(listOf(Any::class, Any::class), GenericKit.getParameterTypeGenericClass(function, 3))

        // Out of bounds
        assertFailsWith<IllegalArgumentException> { GenericKit.getParameterTypeGenericClass(function, 4) }
    }

    @Test
    fun getReturnTypeGeneric() {
        // Property, no generic arguments
        val prop0 = TestGeneric3::class.getMemberProperty("prop0")
        assertEquals(Nothing::class, GenericKit.getReturnTypeGenericClass(prop0, 0))

        // Property with concrete generic arguments
        val prop1 = TestGeneric3::class.getMemberProperty("prop1")
        assertEquals(Int::class, GenericKit.getReturnTypeGenericClass(prop1, 0))
        assertEquals(Float::class, GenericKit.getReturnTypeGenericClass(prop1, 1))
        assertFailsWith<IllegalArgumentException> { GenericKit.getReturnTypeGenericClass(prop1, 2) } // out of bounds

        // Property with generic arguments declared as "*"
        val prop2 = TestGeneric3::class.getMemberProperty("prop2")
        assertEquals(Any::class, GenericKit.getReturnTypeGenericClass(prop2, 0))
        assertEquals(Any::class, GenericKit.getReturnTypeGenericClass(prop2, 1))
        assertFailsWith<IllegalArgumentException> { GenericKit.getReturnTypeGenericClass(prop2, 2) } // out of bounds

        // Function, no generic arguments
        val fun0 = TestGeneric3::class.getMemberFunction("fun0")
        assertEquals(Nothing::class, GenericKit.getReturnTypeGenericClass(fun0, 0))

        // Function with concrete generic arguments
        val fun1 = TestGeneric3::class.getMemberFunction("fun1")
        assertEquals(Int::class, GenericKit.getReturnTypeGenericClass(fun1, 0))
        assertEquals(Float::class, GenericKit.getReturnTypeGenericClass(fun1, 1))
        assertFailsWith<IllegalArgumentException> { GenericKit.getReturnTypeGenericClass(fun1, 2) } // out of bounds

        // Function with generic arguments declared as "*"
        val fun2 = TestGeneric3::class.getMemberFunction("fun2")
        assertEquals(Any::class, GenericKit.getReturnTypeGenericClass(fun2, 0))
        assertEquals(Any::class, GenericKit.getReturnTypeGenericClass(fun2, 1))
        assertFailsWith<IllegalArgumentException> { GenericKit.getReturnTypeGenericClass(fun2, 2) } // out of bounds
    }

    internal class TestGeneric1 : ICallback<Int, Map<String, Boolean>>, Serializable {
        override fun execute(p: Int): Map<String, Boolean> = mapOf()
    }

    internal abstract class TestGeneric : ICallback<Int, Map<String, Boolean>>

    internal class TestGeneric2 : TestGeneric(), Serializable {
        override fun execute(p: Int): Map<String, Boolean> = mapOf()
    }

    internal class TestGeneric3(val prop0: String? = null) : Serializable, HashMap<String, Double>() {

        val prop1: Map<Int, Float>? = null

        val prop2: Map<Any, *>? = null

        fun fun0(): String? = null

        fun fun1(): Map<Int, Float>? = null

        fun fun2(): Map<Any, *>? = null

        @Suppress("UNUSED_PARAMETER")
        fun f(str: String, map: Map<String, Boolean>, map1: Map<Any, *>) {}

    }

}