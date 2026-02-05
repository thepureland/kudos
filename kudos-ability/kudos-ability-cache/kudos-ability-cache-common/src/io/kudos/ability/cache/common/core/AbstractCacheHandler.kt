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

    private var self: AbstractCacheHandler<*>? = null

    /**
     * 返回自身实例，为了解决基于spring aop特性（这里为@Cacheable和@BatchCacheable）的方法在当前类直接调用造成aop失效的问题
     */
    @Suppress("UNCHECKED_CAST")
    protected fun <S : AbstractCacheHandler<*>?> getSelf() : S = self as S ?: SpringKit.getBean(this::class) as S

}