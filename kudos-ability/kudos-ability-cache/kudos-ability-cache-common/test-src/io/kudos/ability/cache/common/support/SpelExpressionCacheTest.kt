package io.kudos.ability.cache.common.support

import org.springframework.expression.spel.support.StandardEvaluationContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * [SpelExpressionCache] 单元测试。
 *
 * 该缓存在 cache 模块所有 Aspect 的热路径上，确保：
 *  - 同一 SpEL 字符串只解析一次（命中缓存返回同一 Expression 实例）
 *  - 不同 SpEL 字符串得到不同实例
 *  - 解析结果可以执行求值（防止有人把 `parseExpression` 换成别的实现而破坏 API）
 */
internal class SpelExpressionCacheTest {

    @Test
    fun get_sameSpel_returnsCachedInstance() {
        val first = SpelExpressionCache.get("#root")
        val second = SpelExpressionCache.get("#root")
        assertSame(first, second, "同一 SpEL 应返回同一缓存的 Expression 实例")
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
