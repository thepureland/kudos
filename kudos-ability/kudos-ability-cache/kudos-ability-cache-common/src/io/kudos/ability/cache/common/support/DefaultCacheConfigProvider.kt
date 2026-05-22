package io.kudos.ability.cache.common.support

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.init.properties.CacheItemsProperties
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties


/**
 * 缓存配置默认提供者：从 `kudos.cache.items` 字符串列表配置（item 形如 `name=xxx&strategy=REMOTE&...`）
 * 和 `cache-item-configs` 结构化配置解析为 [CacheConfig]。
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
        val cacheItemConfigs = itemsProperties.cacheItemConfigs
        log.info("加载到cache-items配置：size=${cacheItems.size}, cache-item-configs配置：size=${cacheItemConfigs.size}")
        if (cacheItems.isNotEmpty()) {
            initCacheConfig(cacheItems)
        }
        if (cacheItemConfigs.isNotEmpty()) {
            initCacheConfigObjects(cacheItemConfigs)
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
            addCacheConfig(cacheConfig, cacheItemStr)
        }
    }

    private fun initCacheConfigObjects(cacheItemConfigs: MutableList<CacheConfig>) {
        cacheItemConfigs.forEachIndexed { index, cacheConfig ->
            addCacheConfig(normalizeDefaults(cacheConfig), "cache-item-configs[$index]")
        }
    }

    private fun cacheItemToConfig(cacheItem: String): CacheConfig {
        val config = CacheConfig().apply {
            writeOnBoot = false
            active = true
        }
        cacheItem.split("&").forEach { param ->
            val (key, value) = parseParam(param, cacheItem)
            require(key in writableConfigProperties) {
                "cache item 包含未知字段 '$key': $cacheItem，可用字段：${writableConfigProperties.joinToString()}"
            }
            BeanKit.setProperty<CacheConfig?>(config, key, value)
        }
        return config
    }

    private fun parseParam(param: String, cacheItem: String): Pair<String, String> {
        val pair = param.split("=", limit = 2)
        require(pair.size == 2 && pair[0].isNotBlank()) {
            "cache item 参数格式错误，应为 key=value: '$param' in '$cacheItem'"
        }
        return pair[0] to pair[1]
    }

    private fun normalizeDefaults(config: CacheConfig): CacheConfig = config.apply {
        if (writeOnBoot == null) {
            writeOnBoot = false
        }
        if (active == null) {
            active = true
        }
    }

    private fun addCacheConfig(cacheConfig: CacheConfig, source: String) {
        // 用派生属性 resolvedStrategyCode 而非裸 `.strategy`——README "新代码必须用派生属性"
        // 契约的最后一处遗留 reader 收口。yml 解析路径只会设 `.strategy`，DB 字典码路径
        // 只会设 `.strategyDictCode`；resolvedStrategyCode 把两条来源兜底集中到一处。
        val strategy = cacheConfig.resolvedStrategyCode
            ?: error("cache item 缺少 strategy: $source")
        require(cacheConfig.resolvedStrategy != null) {
            "cache item strategy 非法 '$strategy': $source，可用值：${CacheStrategy.values().joinToString { it.name }}"
        }
        val name = cacheConfig.name?.takeIf { it.isNotBlank() } ?: error("cache item 缺少 name: $source")
        cacheConfigsByStrategy.getOrPut(strategy) { mutableMapOf() }[name] = cacheConfig
    }

    override fun getCacheConfig(name: String): CacheConfig? = flatConfigs[name]

    override fun getAllCacheConfigs(): Map<String, CacheConfig> = flatConfigs

    override fun getLocalCacheConfigs(): Map<String, CacheConfig> = localConfigs

    override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = remoteConfigs

    override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = localRemoteConfigs

    override fun getHashCacheConfigs(): Map<String, CacheConfig> = hashConfigs

    companion object {
        private val log = LogFactory.getLog(this::class)
        private val writableConfigProperties: Set<String> = CacheConfig::class.memberProperties
            .filterIsInstance<KMutableProperty1<CacheConfig, *>>()
            .map { it.name }
            .toSortedSet()
    }
}
