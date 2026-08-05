package io.kudos.ability.cache.common.support

import io.kudos.context.core.KudosContextHolder
import java.lang.reflect.Method
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Unit tests for [ContextKeyGenerator].
 *
 * Coverage:
 *  - generate() resolves a SpEL key combining a method parameter and the KudosContext (#_context).
 *  - parameter-only expression (#id) works.
 *  - missing @CacheKey -> IllegalArgumentException with the documented message.
 *  - blank @CacheKey value -> IllegalArgumentException with the documented message.
 *  - tenantId taken from the current KudosContext is injected into the key.
 *
 * @author K
 * @since 1.0.0
 */
internal class ContextKeyGeneratorTest {

    private val generator = ContextKeyGenerator()
    private val target = Sample()

    /** Sample target whose methods carry (or omit) the @CacheKey annotation. */
    @Suppress("unused", "UNUSED_PARAMETER")
    class Sample {
        @CacheKey("#_context.tenantId + '::' + #id")
        fun withContext(id: String): String = id

        @CacheKey("#id")
        fun paramOnly(id: String): String = id

        fun noAnnotation(id: String): String = id

        @CacheKey("   ")
        fun blankExpr(id: String): String = id
    }

    private fun method(name: String): Method =
        Sample::class.java.declaredMethods.first { it.name == name }

    @BeforeTest
    fun setup() {
        KudosContextHolder.get().tenantId = "T1"
    }

    @AfterTest
    fun teardown() {
        KudosContextHolder.clear()
    }

    @Test
    fun generate_combinesContextAndParameter() {
        val key = generator.generate(target, method("withContext"), "100")
        assertEquals("T1::100", key)
    }

    @Test
    fun generate_parameterOnlyExpression() {
        val key = generator.generate(target, method("paramOnly"), "abc")
        assertEquals("abc", key)
    }

    @Test
    fun generate_picksUpChangedTenantId() {
        KudosContextHolder.get().tenantId = "T2"
        val key = generator.generate(target, method("withContext"), "9")
        assertEquals("T2::9", key)
    }

    @Test
    fun generate_missingAnnotation_throws() {
        val ex = assertFailsWith<IllegalArgumentException> {
            generator.generate(target, method("noAnnotation"), "x")
        }
        assertEquals("ContextKeyGenerator requires an additional @CacheKey configuration.", ex.message)
    }

    @Test
    fun generate_blankExpression_throws() {
        val ex = assertFailsWith<IllegalArgumentException> {
            generator.generate(target, method("blankExpr"), "x")
        }
        assertEquals("@CacheKey did not specify a rule.", ex.message)
    }
}
