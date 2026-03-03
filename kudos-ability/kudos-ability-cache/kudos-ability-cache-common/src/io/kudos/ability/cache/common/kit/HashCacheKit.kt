package io.kudos.ability.cache.common.kit

import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.core.hash.IHashCache
import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.kit.HashCacheKit.getHashCache
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.base.logger.LogFactory
import io.kudos.context.kit.SpringKit
import org.springframework.stereotype.Component
import kotlin.collections.emptyList

/**
 * Hash 缓存工具：按 cacheName 获取 [IHashCache]（策略封装后的统一抽象）。
 *
 * 与 [KeyValueCacheKit] 类似，使用时通过配置自由选择三种策略（SINGLE_LOCAL / REMOTE / LOCAL_REMOTE）。
 * 若 [cacheName] 未在配置中注册，将抛出 [IllegalStateException]。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
object HashCacheKit {

    private val log = LogFactory.getLog(this)

    /**
     * 是否开启 Hash 缓存。必须是全局开关和指定的缓存开关都开启，才算开启。
     *
     * @param cacheName 缓存名称
     * @return true: 开启缓存，false：未开启缓存
     */
    fun isCacheActive(cacheName: String): Boolean {
        val configProvider = SpringKit.getBeanOrNull(ICacheConfigProvider::class) ?: return false
        val config = configProvider.getHashCacheConfigs()[cacheName] ?: return false
        return config.active == true
    }

    /**
     * 返回指定名称的 Hash 缓存配置信息。
     *
     * @param cacheName 缓存名称
     * @return 缓存配置信息。找不到返回 null
     */
    fun getCacheConfig(cacheName: String): CacheConfig? {
        val configProvider = SpringKit.getBeanOrNull(ICacheConfigProvider::class) ?: return null
        val config = configProvider.getHashCacheConfigs()[cacheName]
        if (config == null) {
            log.warn("Hash 缓存【$cacheName】不存在！")
        }
        return config
    }

    /**
     * 是否在新增或更新后，立即回写 Hash 缓存。
     *
     * @param cacheName 缓存名称
     * @return true: 立即回写缓存, 反之为 false。缓存不存在也返回 false
     */
    fun isWriteInTime(cacheName: String): Boolean {
        if (!isCacheActive(cacheName)) {
            return false
        }
        val cacheConfig = getCacheConfig(cacheName) ?: return false
        return cacheConfig.writeInTime == true
    }

    /**
     * 重新加载所有 Hash 缓存。
     *
     * @param cacheName 缓存名
     */
    fun reloadAll(cacheName: String) {
        if (!isCacheActive(cacheName)) {
            return
        }
        val cacheConfig = getCacheConfig(cacheName) ?: return
        if (cacheConfig.writeOnBoot == true) {
            val beansOfType = SpringKit.getBeansOfType<AbstractHashCacheHandler<*>>()
            beansOfType.values.forEach {
                if (it.cacheName() == cacheName) {
                    it.reloadAll(true)
                }
            }
        } else {
            getHashCache(cacheName).clear(cacheName)
        }
    }

    /**
     * 轻量级判断 Hash 缓存中是否存在指定 id 的实体（不反序列化 value，使用 containsKey/HEXISTS）。
     *
     * @param cacheName 缓存名称
     * @param id        实体 id
     * @return true：存在，false：不存在
     */
    fun existsById(cacheName: String, id: Any): Boolean {
        if (!isCacheActive(cacheName)) {
            return false
        }
        return getHashCache(cacheName).existsById(cacheName, id)
    }

    /**
     * 根据名称获取 Hash 缓存（带 id 对象集合）。
     *
     * @param cacheName 缓存名称（逻辑名，会按版本前缀解析）
     * @return 该名称对应的 IIdEntitiesHashCache
     * @throws IllegalStateException 当 MixHashCacheManager 不可用或该 cacheName 未配置（需在配置中增加名为 cacheName 的项）时
     */
    fun getHashCache(cacheName: String): IHashCache {
        val manager = SpringKit.getBeanOrNull("mixHashCacheManager") as MixHashCacheManager?
            ?: throw IllegalStateException(
                "MixHashCacheManager 不可用（缓存未启用或测试未继承 RdbAndRedisCacheTestBase 等启用缓存的基类）。" +
                    " 请检查 kudos.ability.cache.enabled 或测试上下文。"
            )
        return manager.getHashCache(cacheName)
            ?: throw IllegalStateException("Hash 缓存未配置: 请在缓存配置表sys_cache中增加名为 [$cacheName]的配置项")
    }

    /**
     * 判断指定 Hash 缓存是否启用本地缓存（即同一 key 再次获取可保证返回同一对象引用）。
     * 仅当策略为 [CacheStrategy.LOCAL_REMOTE] 或 [CacheStrategy.SINGLE_LOCAL] 时为 true；
     * [CacheStrategy.REMOTE] 下每次从远程反序列化为新实例，返回 false。
     *
     * @param cacheName 缓存名称（逻辑名，与 [getHashCache] 一致）
     * @return 若未配置或非 LOCAL_REMOTE/SINGLE_LOCAL 则 false
     */
    fun isLocalCacheEnabled(cacheName: String): Boolean {
        val configProvider = SpringKit.getBeanOrNull(ICacheConfigProvider::class) ?: return false
        val config = configProvider.getHashCacheConfigs()[cacheName] ?: return false
        val s = config.strategy ?: config.strategyDictCode ?: return false
        val strategy = try {
            CacheStrategy.valueOf(s)
        } catch (_: Exception) {
            return false
        }
        return strategy == CacheStrategy.LOCAL_REMOTE || strategy == CacheStrategy.SINGLE_LOCAL
    }
}
