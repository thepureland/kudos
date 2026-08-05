package io.kudos.base.lang.reflect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * XKClass extension supplemental test cases (complements XKClassTest).
 *
 * Targets the newInstance argument-matching branches (nullable-param matching a null arg,
 * type-mismatch producing the "no matching constructor" failure, companion-object rejection)
 * and getMemberFunction overload resolution by parameter types.
 *
 * @author K
 * @since 1.0.0
 */
internal class XKClassExtTest {

    @Suppress("unused")
    internal class WithNullable(val name: String?, val age: Int)

    @Suppress("unused")
    internal class Overloaded {
        fun foo(a: Int) {}
        fun foo(a: String) {}
        fun other(d: Double) {}
    }

    internal class WithCompanion {
        companion object
    }

    @Test
    fun newInstance_nullArgMatchesNullableParam() {
        val obj = WithNullable::class.newInstance(null, 5)
        assertEquals(null, obj.name)
        assertEquals(5, obj.age)
    }

    @Test
    fun newInstance_noMatchingConstructor_throws() {
        // correct arg count but wrong types -> no constructor matches -> IllegalArgumentException
        assertFailsWith<IllegalArgumentException> {
            WithNullable::class.newInstance("ok", "notAnInt")
        }
        // wrong arg count -> no constructor matches
        assertFailsWith<IllegalArgumentException> {
            WithNullable::class.newInstance("ok")
        }
    }

    @Test
    fun newInstance_companionObject_rejected() {
        assertFailsWith<IllegalArgumentException> {
            WithCompanion.Companion::class.newInstance()
        }
    }

    @Test
    fun getMemberFunction_overloadResolutionByType() {
        // Two "foo" overloads exist; pass the parameters of the Int overload so the type-matching
        // branch (lines comparing parameter types) selects it.
        val intOverload = Overloaded::class.java.kotlin.members
            .filterIsInstance<kotlin.reflect.KFunction<*>>()
            .first { it.name == "foo" && it.parameters.any { p -> p.type.classifier == Int::class } }
        val resolved = Overloaded::class.getMemberFunction("foo", *intOverload.parameters.toTypedArray())
        assertNotNull(resolved)
        assertEquals(Int::class, resolved.parameters.first { it.kind == kotlin.reflect.KParameter.Kind.VALUE }.type.classifier)
    }

    @Test
    fun getMemberFunction_overloadNoTypeMatch_throws() {
        // there are two "foo" overloads, both with arity 2 (receiver + value). Pass the params of
        // "other" (receiver + Double) which has the same arity but a value type matching neither
        // overload -> the type-matching firstOrNull returns null -> NoSuchElementException.
        val otherParams = Overloaded::class.members
            .filterIsInstance<kotlin.reflect.KFunction<*>>()
            .first { it.name == "other" }
            .parameters
        assertFailsWith<NoSuchElementException> {
            Overloaded::class.getMemberFunction("foo", *otherParams.toTypedArray())
        }
    }

}
