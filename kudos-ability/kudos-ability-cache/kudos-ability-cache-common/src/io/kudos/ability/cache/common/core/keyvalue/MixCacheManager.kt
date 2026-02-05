package io.kudos.ability.cache.common.core.keyvalue

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.core.CacheItemInitializing
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.ability.cache.common.core.keyvalue.IKeyValueCacheManager
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.support.AbstractCacheManager
import java.util.ArrayList
import java.util.Objects

/**
 * 混合缓存管理器
 *
 * 支持两级缓存（本地+远程）的统一管理，根据配置自动选择缓存策略。
 *
 * 核心功能：
 * 1. 缓存策略管理：支持SINGLE_LOCAL、REMOTE、LOCAL_REMOTE三种策略
 * 2. 缓存初始化：在系统初始化完成后加载所有配置的缓存
 * 3. 缓存版本管理：通过CacheVersionConfig支持缓存版本隔离
 * 4. 模式匹配清除：支持按模式（通配符）清除缓存
 *
 * 缓存策略：
 * - SINGLE_LOCAL：仅使用本地缓存（如Caffeine）
 * - REMOTE：仅使用远程缓存（如Redis）
 * - LOCAL_REMOTE：两级缓存，先查本地，未命中再查远程
 *
 * 初始化流程：
 * 1. 检查缓存是否启用
 * 2. 检查缓存管理器是否存在
 * 3. 从配置提供者获取三类配置：本地、远程、混合
 * 4. 初始化本地和远程缓存管理器
 * 5. 加载并注册所有缓存实例
 *
 * 缓存降级/升级：
 * - 如果配置了混合缓存但缺少本地管理器，降级为远程缓存
 * - 如果配置了混合缓存但缺少远程管理器，降级为本地缓存
 * - 自动记录降级/升级日志
 *
 * 注意事项：
 * - 必须在系统初始化完成后调用initCacheAfterSystemInit
 * - 缓存名称会自动添加版本前缀
 * - 模式清除可能影响性能，需谨慎使用
 */
class MixCacheManager : AbstractCacheManager() {

    @Value("\${kudos.ability.cache.enabled}")
    val isCacheEnabled: Boolean? = null

    @Autowired
    private val versionConfig: CacheVersionConfig? = null

    @Autowired(required = false)
    @Qualifier("localCacheManager")
    private val localCacheManager: CacheManager? = null

    @Autowired(required = false)
    @Qualifier("remoteCacheManager")
    private val remoteCacheManager: CacheManager? = null

    @Autowired
    private val cacheConfigProvider: ICacheConfigProvider? = null

    private val caches: MutableList<Cache> = ArrayList<Cache>()

    override fun loadCaches(): Collection<Cache> {
        return caches
    }

    /**
     * 在系统初始化完成后初始化所有缓存
     *
     * 从配置提供者获取所有缓存配置，初始化缓存管理器，并注册所有缓存实例。
     *
     * 工作流程：
     * 1. 检查缓存是否启用：如果未启用，直接返回
     * 2. 检查缓存管理器：如果本地和远程管理器都不存在，直接返回
     * 3. 获取缓存配置：从配置提供者获取三类配置
     *    - localCacheConfigs：仅本地缓存配置
     *    - remoteCacheConfigs：仅远程缓存配置
     *    - localRemoteCacheConfigs：混合缓存配置
     * 4. 初始化缓存管理器：
     *    - 如果本地管理器支持初始化，传入本地配置和混合配置
     *    - 如果远程管理器支持初始化，传入远程配置和混合配置
     * 5. 加载缓存实例：
     *    - 加载本地缓存实例
     *    - 加载远程缓存实例
     *    - 加载混合缓存实例（可能降级）
     * 6. 完成初始化：调用afterPropertiesSet完成Spring缓存管理器初始化
     *
     * 配置合并：
     * - 本地管理器接收：localCacheConfigs + localRemoteCacheConfigs
     * - 远程管理器接收：remoteCacheConfigs + localRemoteCacheConfigs
     * - 混合缓存需要同时初始化本地和远程缓存
     *
     * 调用时机：
     * - 在所有Spring Bean初始化完成后
     * - 由MixCacheInitializing调用
     * - 确保数据库等依赖已准备就绪
     *
     * 注意事项：
     * - 必须在系统初始化完成后调用
     * - 如果缓存未启用，不会加载任何配置
     * - 混合缓存可能因缺少管理器而降级
     */
    fun initCacheAfterSystemInit() {
        if (!this.isCacheEnabled!!) {
            log.warn("缓存未开启,不加载缓存配置.")
            return
        }
        if (localCacheManager == null && remoteCacheManager == null) {
            log.warn("无法找到缓存策略,不加载缓存配置.")
            return
        }
        //查询一次数据，各个缓存组件加载
        val localCacheConfigs = cacheConfigProvider!!.getLocalCacheConfigs()
        val remoteCacheConfigs = cacheConfigProvider.getRemoteCacheConfigs()
        val localRemoteCacheConfigs = cacheConfigProvider.getLocalRemoteCacheConfigs()
        if (localCacheManager != null && localCacheManager is CacheItemInitializing) {
            (localCacheManager as CacheItemInitializing).initCacheAfterSystemInit(
                localCacheConfigs + localRemoteCacheConfigs
            )
        }
        if (remoteCacheManager != null && remoteCacheManager is CacheItemInitializing) {
            (remoteCacheManager as CacheItemInitializing).initCacheAfterSystemInit(
                remoteCacheConfigs + localRemoteCacheConfigs
            )
        }
        caches.addAll(loadLocalCacheConfig(localCacheConfigs))
        caches.addAll(loadRemoteCacheConfig(remoteCacheConfigs))
        caches.addAll(loadMixCacheConfig(localRemoteCacheConfigs))
        afterPropertiesSet()
    }

    override fun getCache(name: String): Cache? {
        val realName = versionConfig!!.getFinalCacheName(name)
        return super.getCache(realName)
    }

    /**
     * 加载本地缓存配置
     *
     * @return List<Cache>
    </Cache> */
    private fun loadLocalCacheConfig(localCacheConfigs: Map<String, CacheConfig>): MutableList<Cache> {
        val localCaches: MutableList<Cache> = ArrayList<Cache>()
        //本地缓存
        if (localCacheManager != null) {
            if (localCacheConfigs.isNotEmpty()) {
                localCacheConfigs.forEach { (key: String, _: CacheConfig?) ->
                    val realKey = versionConfig!!.getFinalCacheName(key)
                    val localCache = localCacheManager.getCache(realKey)
                    localCaches.add(MixCache(CacheStrategy.SINGLE_LOCAL, localCache, null))
                }
            }
        } else {
            log.warn("找不到本地缓存策略，无法加载本地缓存配置！")
        }
        return localCaches
    }

    /**
     * 加载远程缓存配置
     *
     * @return remoteCaches
     */
    private fun loadRemoteCacheConfig(remoteCacheConfigs: Map<String, CacheConfig>): MutableList<Cache> {
        val remoteCaches: MutableList<Cache> = ArrayList<Cache>()
        //远程二级缓存
        if (remoteCacheManager != null) {
            if (remoteCacheConfigs.isNotEmpty()) {
                remoteCacheConfigs.forEach { (key: String, _: CacheConfig?) ->
                    val realKey = versionConfig!!.getFinalCacheName(key)
                    val remoteCache = remoteCacheManager.getCache(realKey)
                    remoteCaches.add(MixCache(CacheStrategy.REMOTE, null, remoteCache))
                }
            }
        } else {
            log.warn("找不远程二级缓存策略，无法加载远程二级缓存配置！")
        }
        return remoteCaches
    }

    /**
     * 加载混合缓存配置
     *
     * 加载本地-远程两级联动缓存配置，根据实际可用的缓存管理器自动降级或升级策略。
     *
     * 工作流程：
     * 1. 遍历所有混合缓存配置
     * 2. 获取最终缓存名称（包含版本前缀）
     * 3. 尝试获取本地和远程缓存实例
     * 4. 根据可用的缓存管理器确定策略：
     *    - 如果本地管理器不存在但远程存在：升级为REMOTE策略
     *    - 如果远程管理器不存在但本地存在：降级为SINGLE_LOCAL策略
     *    - 如果两者都存在：使用LOCAL_REMOTE策略
     * 5. 创建MixCache实例并添加到列表
     *
     * 策略选择：
     * - LOCAL_REMOTE：本地和远程管理器都存在（理想情况）
     * - REMOTE：只有远程管理器（本地管理器缺失，自动升级）
     * - SINGLE_LOCAL：只有本地管理器（远程管理器缺失，自动降级）
     *
     * 降级/升级机制：
     * - 自动适应可用的缓存管理器
     * - 记录降级/升级日志，便于排查问题
     * - 确保缓存功能可用，即使配置不完整
     *
     * 注意事项：
     * - 如果两个管理器都不存在，不会创建缓存实例
     * - 降级/升级会影响缓存性能，应确保配置完整
     * - 缓存名称会自动添加版本前缀
     *
     * @param localRemoteCacheConfigs 混合缓存配置映射
     * @return 混合缓存实例列表
     */
    private fun loadMixCacheConfig(localRemoteCacheConfigs: Map<String, CacheConfig>): MutableList<Cache> {
        val mixCacheConfig: MutableList<Cache> = ArrayList<Cache>()
        // 本地-远程两级联动缓存
        if (localRemoteCacheConfigs.isNotEmpty()) {
            localRemoteCacheConfigs.forEach { (key: String, _: CacheConfig?) ->
                val realKey = versionConfig!!.getFinalCacheName(key)
                val localCache = localCacheManager?.getCache(realKey)
                val remoteCache = remoteCacheManager?.getCache(realKey)
                lateinit var strategy: CacheStrategy
                if (localCacheManager == null) {
                    if (remoteCacheManager != null) {
                        strategy = CacheStrategy.REMOTE
                        log.warn("mix缓存,key={0}升级为远程缓存", key)
                    }
                } else {
                    if (remoteCacheManager != null) {
                        strategy = CacheStrategy.LOCAL_REMOTE
                    } else {
                        strategy = CacheStrategy.SINGLE_LOCAL
                        log.warn("mix缓存,key={0}降级为远程本地缓存", key)
                    }
                }
                mixCacheConfig.add(MixCache(strategy, localCache, remoteCache))
            }
        }
        return mixCacheConfig
    }

    /**
     * 清理本地缓存
     *
     * @param cacheName
     * @param key
     */
    fun clearLocal(cacheName: String, key: Any?) {
        val cache = super.getCache(cacheName) ?: return
        val mixCache = cache as MixCache
        if (key is String
            && key.endsWith("*")
            && localCacheManager != null
        ) {
            (localCacheManager as IKeyValueCacheManager<*>).evictByPattern(cacheName, key)
        } else {
            mixCache.clearLocal(key)
        }
        log.debug("清除本地缓存：{0}::{1}", cacheName, Objects.toString(key, ""))
    }

    /**
     * 按模式清除缓存
     *
     * 根据缓存策略和模式（支持通配符）清除匹配的缓存项。
     *
     * 工作流程：
     * 1. 获取缓存实例：根据缓存名称获取MixCache实例
     * 2. 规范化模式：如果模式不以"*"结尾，自动添加"*"
     * 3. 根据策略清除：
     *    - SINGLE_LOCAL：在本地缓存管理器中按模式清除
     *    - REMOTE：在远程缓存管理器中按模式清除
     *    - LOCAL_REMOTE：在远程缓存管理器中按模式清除，并推送Redis消息通知其他节点
     *
     * 模式匹配：
     * - 支持通配符"*"，例如"user:*"会匹配所有以"user:"开头的key
     * - 如果模式不以"*"结尾，会自动添加"*"进行前缀匹配
     * - 模式匹配可能影响性能，需谨慎使用
     *
     * 分布式同步：
     * - 对于LOCAL_REMOTE策略，清除远程缓存后会推送Redis消息
     * - 其他节点收到消息后会清除本地缓存，保证一致性
     * - 使用pushMsgRedis方法发送通知消息
     *
     * 注意事项：
     * - 模式匹配可能需要扫描所有key，性能开销较大
     * - 对于LOCAL_REMOTE策略，只清除远程缓存，本地缓存通过消息通知清除
     * - 如果缓存不存在，直接返回，不执行任何操作
     *
     * @param cacheName 缓存名称
     * @param pattern 匹配模式，支持通配符"*"
     */
    fun evictByPattern(cacheName: String, pattern: String) {
        val cache = getCache(cacheName) ?: return
        var patternKey = pattern
        if (!patternKey.endsWith("*")) {
            patternKey = "$patternKey*"
        }
        val mixCache = (cache as MixCache)
        if (mixCache.strategy == CacheStrategy.SINGLE_LOCAL) {
            (localCacheManager as IKeyValueCacheManager<*>).evictByPattern(cacheName, patternKey)
        }
        if (mixCache.strategy == CacheStrategy.REMOTE) {
            (remoteCacheManager as IKeyValueCacheManager<*>).evictByPattern(cacheName, patternKey)
        }
        if (mixCache.strategy == CacheStrategy.LOCAL_REMOTE) {
            (remoteCacheManager as IKeyValueCacheManager<*>).evictByPattern(cacheName, patternKey)
            mixCache.pushMsgRedis(cache.getName(), patternKey)
        }
    }

    private val log = LogFactory.getLog(this)

}