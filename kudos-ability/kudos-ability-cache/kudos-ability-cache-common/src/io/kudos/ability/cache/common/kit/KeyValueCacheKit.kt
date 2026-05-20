package io.kudos.ability.cache.common.kit

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.core.keyvalue.MixCache
import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.notify.CacheOperatorVo
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.base.logger.LogFactory
import io.kudos.context.kit.SpringKit
import org.springframework.cache.Cache
import org.springframework.stereotype.Component
import kotlin.reflect.KClass


/**
 * 缓存工具类
 *
 * @author K
 * @since 1.0.0
 */
@Component
object KeyValueCacheKit {

    private val log = LogFactory.getLog(this::class)

    /**
     * 是否开启缓存。必须是全局开关和指定的缓存开关都开启，才算开启
     *
     * @param cacheName 缓存名称
     * @return true: 开启缓存，false：未开启缓存
     * @author K
     * @since 1.0.0
     */
    fun isCacheActive(cacheName: String): Boolean =
        getCacheConfigProvider().getCacheConfig(cacheName)?.isActive ?: false

    /**
     * 根据名称获取缓存
     *
     * @param name 缓存名称
     * @return 缓存对象
     * @author K
     * @since 1.0.0
     */
    fun getCache(name: String): Cache? {
        val cacheManager = getCacheManager() ?: return null
        return cacheManager.getCache(name).also {
            if (it == null) log.error("缓存【$name】不存在！")
        }
    }

    /**
     * 获取缓存中指定key的值
     *
     * @param cacheName  缓存名称
     * @param key        缓存key
     * @param valueClass 缓存key对应的值的类型
     * @return 缓存key对应的值
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> getValue(cacheName: String, key: Any, valueClass: KClass<T>): T? {
        val cache = getCache(cacheName) ?: return null
        return cache.get<T>(key, valueClass.java)
    }

    /**
     * 获取缓存中指定key的值
     *
     * @param cacheName 缓存名称
     * @param key       缓存key
     * @return 缓存key对应的值
     * @author K
     * @since 1.0.0
     */
    fun getValue(cacheName: String, key: Any): Any? = getCache(cacheName)?.get(key)?.get()

    /**
     * 写入缓存
     *
     * @param cacheName 缓存名称
     * @param key       缓存key
     * @param value     要缓存的值
     * @author K
     * @since 1.0.0
     */
    fun put(cacheName: String, key: Any, value: Any?) {
        if (!isCacheActive(cacheName)) return
        getCache(cacheName)?.put(key, value)
    }

    /**
     * 如果不存在，就写入缓存
     *
     * @param cacheName 缓存名称
     * @param key       缓存key
     * @param value     要缓存的值
     * @author K
     * @since 1.0.0
     */
    fun putIfAbsent(cacheName: String, key: Any, value: Any?) {
        if (!isCacheActive(cacheName)) return
        getCache(cacheName)?.putIfAbsent(key, value)
    }

    /**
     * 踢除缓存依赖消息通知
     *
     * @param cacheName 缓存名称
     * @param key       缓存key
     * @author K
     * @since 1.0.0
     */
    fun evict(cacheName: String, key: Any) {
        if (!isCacheActive(cacheName)) return
        val cache = getCache(cacheName) as? MixCache ?: return
        // 单机本地缓存（SINGLE_LOCAL）跨进程不可达，发通知让各节点自行 evict；其他策略远端权威，直接 evict
        if (cache.strategy == CacheStrategy.SINGLE_LOCAL) {
            CacheOperatorVo(CacheOperatorVo.TYPE_EVICT, cacheName, key).doNotify()
        } else {
            doEvict(cacheName, key)
        }
    }

    /**
     * 踢除缓存
     *
     * @param cacheName 缓存名称
     * @param key       缓存key
     * @author K
     * @since 1.0.0
     */
    fun doEvict(cacheName: String, key: Any) {
        if (!isCacheActive(cacheName)) return
        getCache(cacheName)?.evict(key)
    }

    /**
     * 清空缓存，发送消息通知
     *
     * @param cacheName 缓存名称
     * @author K
     * @since 1.0.0
     */
    fun clear(cacheName: String) {
        if (!isCacheActive(cacheName)) return
        val cache = getCache(cacheName) as? MixCache ?: return
        if (cache.strategy == CacheStrategy.SINGLE_LOCAL) {
            CacheOperatorVo(CacheOperatorVo.TYPE_CLEAR, cacheName, null).doNotify()
        } else {
            doClear(cacheName)
        }
    }

    /**
     * 清空缓存
     *
     * @param cacheName 缓存名称
     * @author K
     * @since 1.0.0
     */
    fun doClear(cacheName: String) {
        if (!isCacheActive(cacheName)) return
        getCache(cacheName)?.clear()
    }

    /**
     * 是否在新增或更新后，立即回写缓存
     *
     * @param cacheName 缓存名称
     * @return true: 立即回写缓存, 反之为false。缓存不存在也返回false
     * @author K
     * @since 1.0.0
     */
    fun isWriteInTime(cacheName: String): Boolean {
        if (!isCacheActive(cacheName)) return false
        return getCacheConfig(cacheName)?.isWriteInTime ?: false
    }

    /**
     * 返回指定名称的缓存配置信息
     *
     * @param cacheName 缓存名称
     * @return 缓存配置信息。找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getCacheConfig(cacheName: String): CacheConfig? {
        if (!isCacheActive(cacheName)) return null
        return getCacheConfigProvider().getCacheConfig(cacheName).also {
            if (it == null) log.warn("缓存【$cacheName】不存在！")
        }
    }

    /**
     * 重新加载缓存
     *
     * @param cacheName 缓存名
     * @param key       key
     */
    fun reload(cacheName: String, key: String) {
        if (!isCacheActive(cacheName)) return
        val cacheConfig = getCacheConfig(cacheName) ?: return
        if (cacheConfig.isWriteOnBoot) {
            handlersFor(cacheName).forEach { it.reload(key) }
        } else {
            evict(cacheName, key)
        }
    }

    /**
     * 重新加载所有缓存
     *
     * @param cacheName 缓存名
     */
    fun reloadAll(cacheName: String) {
        if (!isCacheActive(cacheName)) return
        val cacheConfig = getCacheConfig(cacheName) ?: return
        if (cacheConfig.isWriteOnBoot) {
            handlersFor(cacheName).forEach { it.reloadAll(true) }
        } else {
            clear(cacheName)
        }
    }

    /**
     * 收口「按 cacheName 找 Handler」的样板（与 [HashCacheKit.handlersFor] 同思路）。
     *
     * 索引使用 double-checked locking 懒建：首次调用扫一次 + groupBy 落到 [handlerIndex]，
     * 后续调用直接 map 查；新增 handler bean 不会被自动感知，由 [resetForTesting]
     * 或重启上下文触发重建。避免每次 reload / reloadAll 都走 Spring bean 扫描。
     */
    @Volatile private var handlerIndex: Map<String, List<AbstractKeyValueCacheHandler<*>>>? = null

    private fun handlersFor(cacheName: String): List<AbstractKeyValueCacheHandler<*>> {
        val index = handlerIndex ?: synchronized(this) {
            handlerIndex ?: SpringKit.getBeansOfType<AbstractKeyValueCacheHandler<*>>().values
                .groupBy { it.cacheName() }
                .also { handlerIndex = it }
        }
        return index[cacheName].orEmpty()
    }

    /**
     * 清理缓存开头的key
     * @param cacheName 缓存name
     * @param keyPattern key开头
     */
    fun evictByPattern(cacheName: String, keyPattern: String) {
        if (!isCacheActive(cacheName)) return
        val cacheManager = getCacheManager() ?: return
        cacheManager.evictByPattern(cacheName, keyPattern)
    }

    /**
     * 缓存中是否存在指定的key（不依赖 value 是否为 null）,LOCAL_REMOTE 时任一级存在即视为存在。
     *
     * @param cacheName 缓存名称
     * @param key 缓存key
     * @return true：存在， false: 不存在
     */
    fun existsKey(cacheName: String, key: String): Boolean {
        if (!isCacheActive(cacheName)) return false
        return getCacheManager()?.existsKey(cacheName, key) ?: false
    }

    /**
     * 获取缓存管理器
     *
     * 如果缓存未启用（kudos.ability.cache.enabled=false），mixCacheManager Bean 不会被创建，
     * 此时返回 null，调用方需要处理 null 的情况。
     *
     * @return MixCacheManager，如果缓存未启用则返回 null
     */
    private fun getCacheManager(): MixCacheManager? =
        cacheManagerOverride ?: (SpringKit.getBeanOrNull("mixCacheManager") as? MixCacheManager)

    /**
     * 获取缓存配置服务
     *
     * @return ICacheConfigProvider
     */
    private fun getCacheConfigProvider(): ICacheConfigProvider =
        configProviderOverride ?: SpringKit.getBean<ICacheConfigProvider>()

    // ---- 测试注入钩子 ----------------------------------------------------------
    // Kit 是 `object` 单例，生产路径仍走 SpringKit.getBean 查找，行为完全不变。
    // 单元测试若不想拉起 Spring 上下文，可通过下面的 override 注入 mock，再用 resetForTesting 还原。

    @Volatile private var cacheManagerOverride: MixCacheManager? = null
    @Volatile private var configProviderOverride: ICacheConfigProvider? = null

    /**
     * 测试专用：临时注入依赖，避免单测启动完整 Spring 上下文。
     * 任一参数为 null 表示该依赖回退到默认的 [SpringKit] 查找路径。
     * 测试结束必须调用 [resetForTesting] 还原，否则会污染同 JVM 后续测试。
     */
    fun overrideForTesting(
        cacheManager: MixCacheManager? = null,
        configProvider: ICacheConfigProvider? = null,
    ) {
        cacheManagerOverride = cacheManager
        configProviderOverride = configProvider
    }

    /**
     * 测试专用：清掉 [overrideForTesting] 注入的 mock，回到 Spring 查找。
     */
    fun resetForTesting() {
        cacheManagerOverride = null
        configProviderOverride = null
        handlerIndex = null
    }

}