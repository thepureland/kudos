package io.kudos.ability.cache.common.core

import io.kudos.context.kit.SpringKit


/**
 * 业务侧自定义 cache handler 的基类——实现 [cacheName] / [reloadAll] 即可接入框架。
 *
 * 解决两个共性问题：
 *  - **方法间自调用 AOP 失效**：直接 `this.fooCached()` 调用本类的 `@Cacheable` 方法时 AOP 不生效。
 *    [getSelf] 通过 Spring 容器拿到本类的代理实例，调用方写 `getSelf<MyHandler>().fooCached(...)`
 *    就能保证经过代理。
 *  - **重载语义统一**：[reloadAll] 由子类实现，框架在启动期 / 手动失效后统一调它，子类只关心怎么重新装载数据。
 *
 * @author K
 * @since 1.0.0
 */
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
     * 当业务侧把同一个 handler 类注册成多个 bean（不同 qualifier）时，子类**必须** override
     * 本方法返回当前实例应当解析的 bean 名——否则 [selfProxy] 按类型查会撞
     * `NoUniqueBeanDefinitionException`。
     *
     * 默认返回 `null` 表示"按类型唯一查"，覆盖典型场景（一个 handler 类只注册一个 bean）。
     */
    protected open fun selfBeanName(): String? = null

    /**
     * 自身代理实例，[lazy] 委托做线程安全的首次解析。
     *
     * 解析顺序：
     *  1. 若 [selfBeanName] 返回非空 → 按名解析（业务侧多 bean 场景显式指定）
     *  2. 否则按类型解析：[org.springframework.context.ApplicationContext.getBean] 时若同类型仅 1 个 bean 则直接返回；
     *     多个时 [io.kudos.context.kit.SpringKit.getBean] 会按 `@Primary` / 同名匹配规则裁决；都不行就抛
     *     `NoUniqueBeanDefinitionException`——此时业务侧应当 override [selfBeanName]
     *
     * 命名为 `selfProxy` 避免与 [getSelf] 函数的 JVM 签名冲突（属性 getter 默认生成 `getSelf()`）。
     */
    private val selfProxy: AbstractCacheHandler<*> by lazy {
        val name = selfBeanName()
        if (name != null) {
            @Suppress("UNCHECKED_CAST")
            SpringKit.getBean(name) as AbstractCacheHandler<*>
        } else {
            SpringKit.getBean(this::class)
        }
    }

    /**
     * 返回自身实例，为了解决基于spring aop特性（这里为@Cacheable和@BatchCacheable）的方法在当前类直接调用造成aop失效的问题
     */
    @Suppress("UNCHECKED_CAST")
    protected fun <S : AbstractCacheHandler<*>?> getSelf() : S = selfProxy as S

}