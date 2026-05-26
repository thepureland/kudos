package io.kudos.ability.cache.common.support

import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.expression.Expression
import org.springframework.expression.spel.standard.SpelExpressionParser
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-level shared cache for SpEL expressions and base components.
 *
 * Background: every cache-related Aspect / KeyGenerator evaluates SpEL to derive key / condition / unless.
 * In the original implementation:
 * - most Aspects kept [SpelExpressionParser] and [DefaultParameterNameDiscoverer] as class members (OK),
 *   but `ContextKeyGenerator` declared the parser as a local val, allocating a new one on every call.
 * - everywhere repeatedly invoked `parser.parseExpression(...)` on the same SpEL string, each time returning a new
 *   [Expression] instance and incurring a full lex/parse pass. Repeating this on the hot path is wasted work.
 *
 * This object centrally provides:
 * - a singleton parser and discoverer (thread-safe, freely shareable);
 * - [Expression] instances cached by SpEL string, with [ConcurrentHashMap] guaranteeing concurrency safety.
 *
 * `ConcurrentHashMap` is chosen over a weak-reference cache: the set of SpEL strings on annotations is bounded
 * and lives for the application's lifetime.
 */
object SpelExpressionCache {

    private val parser: SpelExpressionParser = SpelExpressionParser()

    /** Discoverer used to resolve method parameter names; stateless and thread-safe. */
    val parameterNameDiscoverer: DefaultParameterNameDiscoverer = DefaultParameterNameDiscoverer()

    private val cache: ConcurrentHashMap<String, Expression> = ConcurrentHashMap()

    /**
     * Returns the [Expression] for the given SpEL string (parsing and caching on first access),
     * avoiding repeated lex/parse work.
     */
    fun get(spel: String): Expression = cache.computeIfAbsent(spel) { parser.parseExpression(it) }
}
