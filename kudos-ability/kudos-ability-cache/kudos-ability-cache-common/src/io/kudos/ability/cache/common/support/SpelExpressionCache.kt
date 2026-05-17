package io.kudos.ability.cache.common.support

import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.expression.Expression
import org.springframework.expression.spel.standard.SpelExpressionParser
import java.util.concurrent.ConcurrentHashMap

/**
 * SpEL 表达式与基础构件的进程级共享缓存。
 *
 * 业务背景：所有 cache 相关的 Aspect / KeyGenerator 都会按 SpEL 取 key / 求 condition / 求 unless。
 * 原实现里：
 * - 大多数 Aspect 把 [SpelExpressionParser] 和 [DefaultParameterNameDiscoverer] 作为类成员（OK），
 *   但 `ContextKeyGenerator` 把 parser 写成局部 val，每次调用都 `new` 一个。
 * - 所有地方都对同一段 SpEL 字符串重复执行 `parser.parseExpression(...)`，每次返回新的 [Expression]
 *   对象，包含一次完整的词法/语法分析。命中路径上重复做这件事属于热点浪费。
 *
 * 这里集中提供：
 * - 单例 parser 与 discoverer（线程安全，可放心共享）。
 * - 按 SpEL 字符串缓存的 [Expression]，[ConcurrentHashMap] 保证并发安全。
 *
 * 选择 `ConcurrentHashMap` 而非弱引用缓存：注解里的 SpEL 字符串数量有限且生命周期与应用一致。
 */
object SpelExpressionCache {

    private val parser: SpelExpressionParser = SpelExpressionParser()

    /** 解析方法参数名用的 discoverer，无状态、线程安全。 */
    val parameterNameDiscoverer: DefaultParameterNameDiscoverer = DefaultParameterNameDiscoverer()

    private val cache: ConcurrentHashMap<String, Expression> = ConcurrentHashMap()

    /**
     * 按 SpEL 字符串获取（首次解析则缓存）对应的 [Expression] 对象，避免重复词法/语法分析。
     */
    fun get(spel: String): Expression = cache.computeIfAbsent(spel) { parser.parseExpression(it) }
}
