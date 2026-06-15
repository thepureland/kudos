package io.kudos.ability.cache.common.support

import io.kudos.context.core.KudosContextHolder
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import java.lang.reflect.Method
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Unit tests for [TenantCacheKeyGenerator].
 *
 * Coverage:
 *  - generalNormalKey (the production path used by TenantCachingAspect):
 *      - non-blank key -> "tenantId::<key-value>".
 *      - blank key -> just the tenantId.
 *      - non-null SpEL evaluation produces a non-null key.
 *  - generate() reads the `suffix` attribute via getAnnotationKey:
 *      - Spring standard @Cacheable / @CacheEvict / @CachePut have no `suffix` attribute -> "" -> tenant only.
 *      - method with no annotation -> tenant only.
 *  - null tenantId on the concat path throws a SpEL evaluation exception (documented limitation: the
 *    "#tenantId.concat(...)" expression cannot operate on a null target).
 *
 * NOTE: The custom @TenantCacheable / @TenantCacheEvict / @TenantCachePut annotations declare `@AliasFor`
 * targets on Cacheable/CacheEvict/CachePut without being meta-annotated with them, so
 * AnnotationUtils.findAnnotation throws AnnotationConfigurationException when they are read here. Production
 * code never reaches that path (TenantCachingAspect passes the nested annotation's `suffix` straight into
 * generalNormalKey). This is recorded as a suspected bug rather than asserted against.
 *
 * @author K
 * @since 1.0.0
 */
internal class TenantCacheKeyGeneratorTest {

    private val generator = TenantCacheKeyGenerator()
    private val target = Sample()

    @Suppress("unused", "UNUSED_PARAMETER")
    class Sample {
        fun noAnnotation(id: String): String = id

        @Cacheable("c1")
        fun springCacheable(id: String): String = id

        @CacheEvict("c1")
        fun springEvict(id: String): String = id

        @CachePut("c1")
        fun springPut(id: String): String = id
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

    // ---- generalNormalKey ---------------------------------------------------

    @Test
    fun generalNormalKey_nonBlankKey_prefixesTenant() {
        val key = generator.generalNormalKey(target, method("noAnnotation"), "#id", "100")
        assertEquals("T1::100", key)
    }

    @Test
    fun generalNormalKey_blankKey_returnsTenantOnly() {
        val key = generator.generalNormalKey(target, method("noAnnotation"), "", "100")
        assertEquals("T1", key)
    }

    @Test
    fun generalNormalKey_literalSuffix() {
        val key = generator.generalNormalKey(target, method("noAnnotation"), "'static'", "ignored")
        assertEquals("T1::static", key)
    }

    @Test
    fun generalNormalKey_nullTenant_concatThrows() {
        // Documented limitation: "#tenantId.concat(...)" cannot run when tenantId is null.
        KudosContextHolder.get().tenantId = null
        assertFailsWith<org.springframework.expression.spel.SpelEvaluationException> {
            generator.generalNormalKey(target, method("noAnnotation"), "#id", "5")
        }
    }

    // ---- generate via getAnnotationKey (standard Spring annotations) --------

    @Test
    fun generate_springCacheable_noSuffix_returnsTenantOnly() {
        // Standard @Cacheable has no `suffix` attribute -> getAnnotationKey returns "" -> just the tenant id.
        assertEquals("T1", generator.generate(target, method("springCacheable"), "x"))
    }

    @Test
    fun generate_springCacheEvict_noSuffix_returnsTenantOnly() {
        assertEquals("T1", generator.generate(target, method("springEvict"), "x"))
    }

    @Test
    fun generate_springCachePut_noSuffix_returnsTenantOnly() {
        assertEquals("T1", generator.generate(target, method("springPut"), "x"))
    }

    @Test
    fun generate_noAnnotation_returnsTenantOnly() {
        assertEquals("T1", generator.generate(target, method("noAnnotation"), "x"))
    }

    @Test
    fun generate_nullTenant_noSuffix_requireNotNullThrows() {
        // No annotation -> key expression is "#tenantId"; with a null tenant it evaluates to null and the
        // requireNotNull guard throws IllegalArgumentException with the documented message.
        KudosContextHolder.get().tenantId = null
        val ex = assertFailsWith<IllegalArgumentException> {
            generator.generate(target, method("noAnnotation"), "x")
        }
        assertEquals("SpEL cache key must resolve to non-null String", ex.message)
    }
}
