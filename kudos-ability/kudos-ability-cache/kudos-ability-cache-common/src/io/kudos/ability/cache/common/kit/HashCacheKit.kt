package io.kudos.ability.cache.common.kit

import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.core.hash.IHashCache
import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.kit.HashCacheKit.getHashCache
import io.kudos.ability.cache.common.notify.CacheOperatorVo
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.context.kit.SpringKit
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

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

    private val log = LogFactory.getLog(this::class)

    /**
     * 是否开启 Hash 缓存。必须是全局开关和指定的缓存开关都开启，才算开启。
     *
     * @param cacheName 缓存名称
     * @return true: 开启缓存，false：未开启缓存
     */
    fun isCacheActive(cacheName: String): Boolean =
        getConfigProvider()?.getHashCacheConfigs()?.get(cacheName)?.isActive == true

    /**
     * 返回指定名称的 Hash 缓存配置信息。
     *
     * @param cacheName 缓存名称
     * @return 缓存配置信息。找不到返回 null
     */
    fun getCacheConfig(cacheName: String): CacheConfig? {
        val configProvider = getConfigProvider() ?: return null
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
        if (!isCacheActive(cacheName)) return false
        return getCacheConfig(cacheName)?.isWriteInTime == true
    }

    /**
     * 踢除指定 id 的 Hash 缓存（若为 SINGLE_LOCAL 会发通知，否则直接删除）。
     *
     * @param cacheName 缓存名
     * @param id        实体主键
     */
    fun evict(cacheName: String, id: Any) {
        if (!isCacheActive(cacheName)) return
        val config = getCacheConfig(cacheName) ?: return
        if (config.resolvedStrategy == CacheStrategy.SINGLE_LOCAL) {
            CacheOperatorVo(CacheOperatorVo.TYPE_EVICT, cacheName, id).doNotify()
        } else {
            doEvict(cacheName, id)
        }
    }

    /**
     * 踢除指定 id 的 Hash 缓存（直接删除，不发通知）。
     *
     * @param cacheName 缓存名
     * @param id        实体主键
     */
    fun doEvict(cacheName: String, id: Any) {
        if (!isCacheActive(cacheName)) return
        handlersFor(cacheName).forEach { it.evict(id) }
    }

    /**
     * 清空 Hash 缓存（若为 SINGLE_LOCAL 会发通知，否则直接清空）。
     *
     * @param cacheName 缓存名
     */
    fun clear(cacheName: String) {
        if (!isCacheActive(cacheName)) return
        val config = getCacheConfig(cacheName) ?: return
        if (config.resolvedStrategy == CacheStrategy.SINGLE_LOCAL) {
            CacheOperatorVo(CacheOperatorVo.TYPE_CLEAR, cacheName, null).doNotify()
        } else {
            doClear(cacheName)
        }
    }

    /**
     * 清空 Hash 缓存（直接清空，不发通知）。
     *
     * @param cacheName 缓存名
     */
    fun doClear(cacheName: String) {
        if (!isCacheActive(cacheName)) return
        getHashCache(cacheName).clear(cacheName)
    }

    /**
     * 重新加载指定 id 的 Hash 缓存。
     * 委托该 cacheName 对应的 Handler：先从 Hash 中删除该 id，若配置了 writeOnBoot 且 Handler 实现了 [AbstractHashCacheHandler.doReload] 则从数据源加载并回写。
     *
     * @param cacheName 缓存名
     * @param id        实体主键
     */
    fun reload(cacheName: String, id: Any) {
        if (!isCacheActive(cacheName)) return
        handlersFor(cacheName).forEach { it.reload(id) }
    }

    /**
     * 重新加载所有 Hash 缓存。
     *
     * @param cacheName 缓存名
     */
    fun reloadAll(cacheName: String) {
        if (!isCacheActive(cacheName)) return
        val cacheConfig = getCacheConfig(cacheName) ?: return
        if (cacheConfig.isWriteOnBoot) {
            handlersFor(cacheName).forEach { it.reloadAll(true) }
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
        if (!isCacheActive(cacheName)) return false
        return getHashCache(cacheName).existsById(cacheName, id)
    }

    /**
     * 获取 Hash 缓存中指定 id 的实体值（带类型）。
     *
     * @param cacheName  缓存名称
     * @param id         实体主键
     * @param valueClass 实体类型
     * @return 实体，不存在时返回 null
     */
    fun <PK, T : IIdEntity<PK>> getValue(cacheName: String, id: PK, valueClass: KClass<T>): T? {
        if (!isCacheActive(cacheName)) return null
        return getHashCache(cacheName).getById(cacheName, id, valueClass)
    }

    /**
     * 获取 Hash 缓存中指定 id 的实体值（无类型，返回 Any?）。
     * 内部通过该 cacheName 对应的 Handler 的实体类型反序列化。
     *
     * @param cacheName 缓存名称
     * @param id        实体主键
     * @return 实体，不存在或未配置对应 Handler 时返回 null
     */
    fun getValue(cacheName: String, id: Any): Any? {
        if (!isCacheActive(cacheName)) return null
        val handler = handlersFor(cacheName).firstOrNull() ?: return null
        @Suppress("UNCHECKED_CAST")
        val entityClass = handler.exposedEntityClass() as KClass<IIdEntity<Any?>>
        return getHashCache(cacheName).getById(cacheName, id, entityClass)
    }

    /**
     * 收口「按 cacheName 找 Handler」的样板：扫 [AbstractHashCacheHandler] 类型的全部 bean，
     * 取 `cacheName()` 匹配的子集。N 个调用点之前各自手写 `forEach { if (...) ... }`，
     * 现在统一走这里——新增 handler 类型时只此一处需要改。
     */
    private fun handlersFor(cacheName: String): List<AbstractHashCacheHandler<*>> =
        SpringKit.getBeansOfType<AbstractHashCacheHandler<*>>().values
            .filter { it.cacheName() == cacheName }

    /**
     * 根据名称获取 Hash 缓存（带 id 对象集合）。
     *
     * @param cacheName 缓存名称（逻辑名，会按版本前缀解析）
     * @return 该名称对应的 IIdEntitiesHashCache
     * @throws IllegalStateException 当 MixHashCacheManager 不可用或该 cacheName 未配置（需在配置中增加名为 cacheName 的项）时
     */
    fun getHashCache(cacheName: String): IHashCache {
        val manager = getManager()
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
        val strategy = getConfigProvider()?.getHashCacheConfigs()?.get(cacheName)?.resolvedStrategy ?: return false
        return strategy == CacheStrategy.LOCAL_REMOTE || strategy == CacheStrategy.SINGLE_LOCAL
    }

    // ---- 测试注入钩子 ----------------------------------------------------------
    // 与 [KeyValueCacheKit] 同样的模式：默认走 SpringKit 查找，测试可通过 override 注入 mock。
    // 注意：基于类型遍历的 Handler 查找（reload / reloadAll / getValue(no type)）仍走 Spring，
    //      因为按 cacheName 匹配 handler 的语义只在真实 bean 容器中清晰；要测这些路径建议起 Spring。

    @Volatile private var managerOverride: MixHashCacheManager? = null
    @Volatile private var configProviderOverride: ICacheConfigProvider? = null

    private fun getManager(): MixHashCacheManager? =
        managerOverride ?: SpringKit.getBeanOrNull("mixHashCacheManager") as MixHashCacheManager?

    private fun getConfigProvider(): ICacheConfigProvider? =
        configProviderOverride ?: SpringKit.getBeanOrNull(ICacheConfigProvider::class)

    /**
     * 测试专用：临时注入依赖，避免单测启动完整 Spring 上下文。
     * 任一参数为 null 表示该依赖回退到默认的 [SpringKit] 查找路径。
     * 测试结束必须调用 [resetForTesting] 还原。
     */
    fun overrideForTesting(
        manager: MixHashCacheManager? = null,
        configProvider: ICacheConfigProvider? = null,
    ) {
        managerOverride = manager
        configProviderOverride = configProvider
    }

    /**
     * 测试专用：清掉 [overrideForTesting] 注入的 mock，回到 Spring 查找。
     */
    fun resetForTesting() {
        managerOverride = null
        configProviderOverride = null
    }
}
