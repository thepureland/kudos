package io.kudos.ability.cache.common.support

import org.springframework.expression.spel.support.StandardEvaluationContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Unit tests for [SpelExpressionCache].
 *
 * This cache sits on the hot path of every Aspect in the cache module, ensuring:
 *  - the same SpEL string is parsed only once (a hit returns the same Expression instance);
 *  - different SpEL strings yield different instances;
 *  - parsed results can still be evaluated (so that swapping `parseExpression` for some other implementation cannot silently break the API).
 */
internal class SpelExpressionCacheTest {

    @Test
    fun get_sameSpel_returnsCachedInstance() {
        val first = SpelExpressionCache.get("#root")
        val second = SpelExpressionCache.get("#root")
        assertSame(first, second, "The same SpEL should return the same cached Expression instance")
    }

    @Test
    fun get_differentSpel_returnsDifferentInstances() {
        val a = SpelExpressionCache.get("'foo'")
        val b = SpelExpressionCache.get("'bar'")
        assert(a !== b)
    }

    @Test
    fun get_compilesEvaluableExpression() {
        val expr = SpelExpressionCache.get("1 + 2")
        val ctx = StandardEvaluationContext()
        assertEquals(3, expr.getValue(ctx, Int::class.java))
    }

    @Test
    fun get_supportsStringConcat() {
        val expr = SpelExpressionCache.get("'kudos:' + #name")
        val ctx = StandardEvaluationContext().apply { setVariable("name", "cache") }
        assertEquals("kudos:cache", expr.getValue(ctx, String::class.java))
    }
}
