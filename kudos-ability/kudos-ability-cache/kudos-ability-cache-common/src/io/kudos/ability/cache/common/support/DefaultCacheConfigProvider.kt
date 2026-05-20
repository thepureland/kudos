package io.kudos.ability.cache.common.support

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.init.properties.CacheItemsProperties
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory


/**
 * 缓存配置默认提供者：从 `kudos.cache.items` 这种字符串列表配置（item 形如 `name=xxx&strategy=REMOTE&...`）解析为 [CacheConfig]。
 *
 * 关键点：
 * - 配置在 init 阶段一次性解析，运行期不变 → 所有查询都基于初始化后构建的扁平 map，O(1) 命中。
 * - 旧实现把 cacheConfigs 放在 companion object 里（多实例会共享状态，跨测试上下文会"配置累积"），现在改成实例字段。
 * - 旧实现的 [getCacheConfig] 和 [getAllCacheConfigs] 每次都重建扁平 map（O(策略数 × 缓存项数)），
 *   而 Kit 在热路径上反复调它们；现在改成构建一次后只读，避免重复聚合开销。
 */
class DefaultCacheConfigProvider(itemsProperties: CacheItemsProperties) : ICacheConfigProvider {

    /** 按策略分组的配置（结构与旧实现一致，仅作为构造期数据组织）。 */
    private val cacheConfigsByStrategy: MutableMap<String, MutableMap<String, CacheConfig>> = mutableMapOf()

    /** 扁平化的全量配置 cacheName -> config，初始化后冻结。 */
    private val flatConfigs: Map<String, CacheConfig>

    /** 按策略预切的不可变视图，避免每次查询再过滤。 */
    private val localConfigs: Map<String, CacheConfig>
    private val remoteConfigs: Map<String, CacheConfig>
    private val localRemoteConfigs: Map<String, CacheConfig>
    private val hashConfigs: Map<String, CacheConfig>

    init {
        val cacheItems = itemsProperties.cacheItems
        log.info("加载到cache-items配置：size=${cacheItems.size}")
        if (cacheItems.isNotEmpty()) {
            initCacheConfig(cacheItems)
        }

        flatConfigs = cacheConfigsByStrategy.values.flatMap { it.entries }
            .associate { it.key to it.value }
        localConfigs = (cacheConfigsByStrategy[CacheStrategy.SINGLE_LOCAL.name] ?: emptyMap()).toMap()
        remoteConfigs = (cacheConfigsByStrategy[CacheStrategy.REMOTE.name] ?: emptyMap()).toMap()
        localRemoteConfigs = (cacheConfigsByStrategy[CacheStrategy.LOCAL_REMOTE.name] ?: emptyMap()).toMap()
        hashConfigs = flatConfigs.filterValues { it.hash }
    }

    /**
     * 从配置解析，初始化缓存
     *
     * @param cacheItems
     */
    private fun initCacheConfig(cacheItems: MutableList<String>) {
        cacheItems.filter { it.isNotBlank() }.forEach { cacheItemStr ->
            val cacheConfig = cacheItemToConfig(cacheItemStr)
            // 用派生属性 resolvedStrategyCode 而非裸 `.strategy`——README "新代码必须用派生属性"
            // 契约的最后一处遗留 reader 收口。yml 解析路径只会设 `.strategy`，DB 字典码路径
            // 只会设 `.strategyDictCode`；resolvedStrategyCode 把两条来源兜底集中到一处。
            val strategy = cacheConfig.resolvedStrategyCode
                ?: error("cache item 缺少 strategy: $cacheItemStr")
            val name = cacheConfig.name ?: error("cache item 缺少 name: $cacheItemStr")
            cacheConfigsByStrategy.getOrPut(strategy) { mutableMapOf() }[name] = cacheConfig
        }
    }

    /**
     * 解析"key=val&key=val"形式的配置串为 [CacheConfig]，给 yml 配置里把缓存项写成单行 query-string 用。
     *
     * 默认值：`writeOnBoot=false` / `active=true`（开缓存但不预热），由 [BeanKit.setProperty] 按 key
     * 反射 setter 覆盖。
     *
     * @param cacheItem 形如 `name=foo&strategy=LOCAL_REMOTE&ttl=600` 的配置串
     * @return 解析好的 [CacheConfig]
     * @author K
     * @since 1.0.0
     */
    private fun cacheItemToConfig(cacheItem: String): CacheConfig {
        val config = CacheConfig().apply {
            writeOnBoot = false
            active = true
        }
        cacheItem.split("&").forEach { param ->
            val (key, value) = param.split("=", limit = 2)
            BeanKit.setProperty<CacheConfig?>(config, key, value)
        }
        return config
    }

    override fun getCacheConfig(name: String): CacheConfig? = flatConfigs[name]

    override fun getAllCacheConfigs(): Map<String, CacheConfig> = flatConfigs

    override fun getLocalCacheConfigs(): Map<String, CacheConfig> = localConfigs

    override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = remoteConfigs

    override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = localRemoteConfigs

    override fun getHashCacheConfigs(): Map<String, CacheConfig> = hashConfigs

    companion object {
        private val log = LogFactory.getLog(this::class)
    }
}
