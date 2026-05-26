package io.kudos.test.rdb

import io.kudos.context.kit.SpringKit

/**
 * Provides a unified "reset caches after test data is persisted" capability for cache-related integration tests.
 *
 * Cache components are intentionally accessed via reflection instead of adding direct dependencies on business cache modules to `kudos-test-rdb`.
 * This keeps the test infrastructure in the common test layer without coupling it backward to concrete business modules.
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
     * Resets the shared Redis container and the cache state of the current Spring context to the snapshot corresponding to the "current test SQL".
     *
     * Steps:
     * 1. Flush Redis to remove remote cache entries left over from other test classes.
     * 2. Read the current cache configuration from the database.
     * 3. Perform clear + reload per cache type to restore local/remote cache contents.
     */
    fun resetRedisAndApplicationCaches() {
        flushRedis()

        val cacheConfigProvider = getBeanByClassName(CACHE_CONFIG_PROVIDER_CLASS) ?: return
        val allConfigs = invoke(cacheConfigProvider, "getAllCacheConfigs") as? Map<*, *> ?: return
        resetCaches(allConfigs)
    }

    /**
     * Flushes the current test Redis DB.
     *
     * The test container is reused; without an initial flush, keys left by a previous test class would pollute subsequent assertions.
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
     * Separates plain caches from hash caches based on `sys_cache` configuration and clears/reloads each set.
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
     * Filters cache names of the specified type from the cache configuration.
     */
    private fun Map<*, *>.filterCacheNames(expectHash: Boolean): List<String> {
        return entries
            .filter { (_, config) -> readHashFlag(config) == expectHash }
            .mapNotNull { (name, _) -> name as? String }
    }

    /**
     * Invokes the cache kit's `doClear` to uniformly clear the target caches.
     */
    private fun clearCaches(kitClassName: String, cacheNames: List<String>) {
        cacheNames.forEach { invokeStatic(kitClassName, "doClear", it) }
    }

    /**
     * Invokes the cache kit's `reloadAll` to restore caches to the state corresponding to current database data.
     */
    private fun reloadCaches(kitClassName: String, cacheNames: List<String>) {
        cacheNames.forEach { invokeStatic(kitClassName, "reloadAll", it) }
    }

    /**
     * Reads the `hash` flag from the cache config object; on failure, treats it as a plain cache.
     */
    private fun readHashFlag(config: Any?): Boolean {
        if (config == null) return false
        return invokeGetter(config, "hash") as? Boolean ?: false
    }

    /**
     * Fetches a bean from the Spring container by class name.
     *
     * Returning null means the current test context has no such component; callers should treat it as "no reset needed for this capability".
     */
    private fun getBeanByClassName(className: String): Any? {
        val beanClass = runCatching { Class.forName(className) }.getOrNull() ?: return null
        val ctx = runCatching { SpringKit.applicationContext }.getOrNull() ?: return null
        return runCatching { ctx.getBean(beanClass) }.getOrNull()
    }

    /**
     * Reads a property using the JavaBean getter convention to avoid depending on the target type directly.
     */
    private fun invokeGetter(target: Any, propertyName: String): Any? {
        val getterName = "get${propertyName.replaceFirstChar { it.uppercase() }}"
        return invoke(target, getterName)
    }

    /**
     * Invokes an instance method using loose parameter matching.
     *
     * The goal here is test-infrastructure compatibility, not strict typing; any runtime-assignable method signature is acceptable.
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
     * Invokes a method on a Kotlin `object` singleton.
     */
    private fun invokeStatic(className: String, methodName: String, vararg args: Any?) {
        val clazz = runCatching { Class.forName(className) }.getOrNull() ?: return
        val instance = clazz.getField("INSTANCE").get(null)
        invoke(instance, methodName, *args)
    }
}
