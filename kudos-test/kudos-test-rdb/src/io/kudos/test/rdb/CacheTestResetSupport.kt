package io.kudos.test.rdb

import io.kudos.context.kit.SpringKit

/**
 * 为缓存相关集成测试提供统一的“测试数据落库后缓存重置”能力。
 *
 * 这里故意通过反射访问缓存组件，而不是给 `kudos-test-rdb` 直接增加业务缓存模块依赖。
 * 这样测试基础设施仍然保持在公共测试层，不会反向耦合到具体业务模块。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal object CacheTestResetSupport {

    private const val CACHE_CONFIG_PROVIDER_CLASS = "io.kudos.ability.cache.common.support.ICacheConfigProvider"
    private const val REDIS_TEMPLATES_CLASS = "io.kudos.ability.data.memdb.redis.RedisTemplates"
    private const val KEY_VALUE_CACHE_KIT_CLASS = "io.kudos.ability.cache.common.kit.KeyValueCacheKit"
    private const val HASH_CACHE_KIT_CLASS = "io.kudos.ability.cache.common.kit.HashCacheKit"

    /**
     * 将共享 Redis 容器和当前 Spring Context 中的缓存状态重置到“当前测试 SQL”对应的快照。
     *
     * 步骤是：
     * 1. 先清空 Redis，去掉跨测试类残留的远端缓存。
     * 2. 读取当前数据库中的缓存配置。
     * 3. 按缓存类型分别执行 clear + reload，恢复本地/远端缓存内容。
     */
    fun resetRedisAndApplicationCaches() {
        flushRedis()

        val cacheConfigProvider = getBeanByClassName(CACHE_CONFIG_PROVIDER_CLASS) ?: return
        val allConfigs = invoke(cacheConfigProvider, "getAllCacheConfigs") as? Map<*, *> ?: return
        resetCaches(allConfigs)
    }

    /**
     * 直接清空当前测试 Redis DB。
     *
     * 测试容器是复用的；如果不先 flush，前一个测试类遗留的 key 会污染后续断言。
     */
    private fun flushRedis() {
        val redisTemplates = getBeanByClassName(REDIS_TEMPLATES_CLASS) ?: return
        val defaultRedisTemplate = invokeGetter(redisTemplates, "defaultRedisTemplate") ?: return
        val connectionFactory = invokeGetter(defaultRedisTemplate, "connectionFactory") ?: return
        val connection = invoke(connectionFactory, "getConnection") ?: return
        try {
            val serverCommands = invoke(connection, "serverCommands") ?: return
            invoke(serverCommands, "flushDb")
        } finally {
            invoke(connection, "close")
        }
    }

    /**
     * 根据 `sys_cache` 配置区分普通缓存和 hash 缓存，并分别执行清理与重载。
     */
    private fun resetCaches(allConfigs: Map<*, *>) {
        val keyValueCacheNames = allConfigs.filterCacheNames(expectHash = false)
        val hashCacheNames = allConfigs.filterCacheNames(expectHash = true)

        clearCaches(KEY_VALUE_CACHE_KIT_CLASS, keyValueCacheNames)
        clearCaches(HASH_CACHE_KIT_CLASS, hashCacheNames)

        reloadCaches(KEY_VALUE_CACHE_KIT_CLASS, keyValueCacheNames)
        reloadCaches(HASH_CACHE_KIT_CLASS, hashCacheNames)
    }

    /**
     * 从缓存配置中筛出指定类型的缓存名。
     */
    private fun Map<*, *>.filterCacheNames(expectHash: Boolean): List<String> {
        return entries
            .filter { (_, config) -> readHashFlag(config) == expectHash }
            .mapNotNull { (name, _) -> name as? String }
    }

    /**
     * 调用缓存工具类的 `doClear`，统一清空目标缓存。
     */
    private fun clearCaches(kitClassName: String, cacheNames: List<String>) {
        cacheNames.forEach { invokeStatic(kitClassName, "doClear", it) }
    }

    /**
     * 调用缓存工具类的 `reloadAll`，把缓存恢复到当前数据库数据对应的状态。
     */
    private fun reloadCaches(kitClassName: String, cacheNames: List<String>) {
        cacheNames.forEach { invokeStatic(kitClassName, "reloadAll", it) }
    }

    /**
     * 从缓存配置对象里读取 `hash` 标记；读取失败时按普通缓存处理。
     */
    private fun readHashFlag(config: Any?): Boolean {
        if (config == null) return false
        return invokeGetter(config, "hash") as? Boolean ?: false
    }

    /**
     * 通过类名从 Spring 容器中取 Bean。
     *
     * 返回 null 表示当前测试上下文没有这类组件，调用方按“无需重置该部分能力”处理。
     */
    private fun getBeanByClassName(className: String): Any? {
        val beanClass = runCatching { Class.forName(className) }.getOrNull() ?: return null
        val ctx = runCatching { SpringKit.applicationContext }.getOrNull() ?: return null
        return runCatching { ctx.getBean(beanClass) }.getOrNull()
    }

    /**
     * 以 JavaBean getter 约定读取属性，避免直接依赖目标类型。
     */
    private fun invokeGetter(target: Any, propertyName: String): Any? {
        val getterName = "get${propertyName.replaceFirstChar { it.uppercase() }}"
        return invoke(target, getterName)
    }

    /**
     * 通过宽松的参数匹配执行实例方法。
     *
     * 这里的目标是测试基础设施的兼容性，不追求强类型；只要运行时能找到可赋值的方法签名即可。
     */
    private fun invoke(target: Any, methodName: String, vararg args: Any?): Any? {
        val parameterTypes = args.map { it?.javaClass ?: Any::class.java }.toTypedArray()
        val method = target.javaClass.methods.firstOrNull { method ->
            method.name == methodName &&
                method.parameterCount == args.size &&
                method.parameterTypes.zip(parameterTypes).all { (expected, actual) ->
                    actual == Any::class.java || expected.isAssignableFrom(actual)
                }
        } ?: return null
        method.isAccessible = true
        return method.invoke(target, *args)
    }

    /**
     * 调用 Kotlin `object` 单例上的方法。
     */
    private fun invokeStatic(className: String, methodName: String, vararg args: Any?) {
        val clazz = runCatching { Class.forName(className) }.getOrNull() ?: return
        val instance = clazz.getField("INSTANCE").get(null)
        invoke(instance, methodName, *args)
    }
}
