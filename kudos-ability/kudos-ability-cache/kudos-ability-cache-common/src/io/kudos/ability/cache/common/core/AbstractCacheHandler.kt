package io.kudos.ability.cache.common.core

import io.kudos.context.kit.SpringKit


abstract class AbstractCacheHandler<T> {

    /**
     * 返回缓存名称
     *
     * @return 缓存名称
     */
    abstract fun cacheName(): String

    /**
     * 重载所有缓存
     *
     * @param clear 重载前是否先清除
     */
    abstract fun reloadAll(clear: Boolean = true)

    /**
     * 自身代理实例，通过 [lazy] 委托做线程安全的首次解析。
     * 旧实现是 `var self = null` + `self as S ?: getBean(...)` 的"伪 DCL"：字段从未被赋值，每次调用都反射查 bean；
     * 同时缺 `@Volatile`，并发下也不安全。改成 lazy 后只解析一次，且 JVM 保证发布安全。
     * 命名为 selfProxy 避免与 [getSelf] 函数的 JVM 签名冲突（属性 getter 默认会生成 getSelf()）。
     */
    private val selfProxy: AbstractCacheHandler<*> by lazy { SpringKit.getBean(this::class) }

    /**
     * 返回自身实例，为了解决基于spring aop特性（这里为@Cacheable和@BatchCacheable）的方法在当前类直接调用造成aop失效的问题
     */
    @Suppress("UNCHECKED_CAST")
    protected fun <S : AbstractCacheHandler<*>?> getSelf() : S = selfProxy as S

}