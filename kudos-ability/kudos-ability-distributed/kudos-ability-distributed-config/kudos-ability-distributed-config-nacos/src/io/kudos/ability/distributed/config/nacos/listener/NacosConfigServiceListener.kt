package io.kudos.ability.distributed.config.nacos.listener

import com.alibaba.nacos.api.NacosFactory
import com.alibaba.nacos.api.PropertyKeyConst
import com.alibaba.nacos.api.config.ConfigService
import com.alibaba.nacos.api.exception.NacosException
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

/**
 * Nacos 配置文件监听器——`ConfigService` 的轻封装，给业务侧 `addListener` /
 * `removeListener` 两个最常用操作。
 *
 * **多集群支持**：[ConfigService] 是按 `(serverAddr, namespace)` 分桶缓存的进程级 Map
 * （见 [SERVICE_CACHE]），不同 Nacos 集群可以并存。同一对 `(serverAddr, namespace)` 仍然
 * 复用一份重型对象，避免 SDK 内部 HTTP / gRPC 客户端 + 调度线程的重复开销。
 *
 * 历史背景：旧实现把 `configService` 做成 `@Volatile var` 单例 + 双重检查 init——
 * 首次初始化的 properties 胜出，后续构造静默忽略；同一进程内挂两个不同 Nacos 集群
 * 直接错乱。本次修复改为分桶缓存。
 *
 * @author hanson
 * @author K
 * @since 1.0.0
 */
class NacosConfigServiceListener {

    private val configService: ConfigService

    constructor(serverAddr: String?) {
        require(!serverAddr.isNullOrBlank()) { "serverAddr must not be blank" }
        val propertiesBuilder = PropertiesBuilder()
        propertiesBuilder.put(PRO_SERVER_ADDR_KEY, serverAddr)
        configService = obtainConfigService(propertiesBuilder.get())
    }

    constructor(propertiesBuilder: PropertiesBuilder) {
        configService = obtainConfigService(propertiesBuilder.get())
    }

    /**
     * 注册一个 dataId + group 的配置变更监听器。
     *
     * @param dataId Nacos dataId
     * @param group Nacos group
     * @param listener Nacos 提供的抽象监听器（业务侧实现 onChange 即可）
     * @throws NacosException Nacos SDK 抛出
     * @throws IllegalArgumentException 当 [configService] 尚未初始化时
     * @author K
     * @since 1.0.0
     */
    @Throws(NacosException::class)
    fun addListener(dataId: String?, group: String?, listener: AbstractConfigChangeListener?) {
        configService.addListener(dataId, group, listener)
    }

    /**
     * 注销之前 [addListener] 注册的监听器。
     *
     * @param dataId Nacos dataId
     * @param group Nacos group
     * @param listener 要移除的监听器实例（必须与注册时同一引用）
     * @throws IllegalArgumentException 当 [configService] 尚未初始化时
     * @author K
     * @since 1.0.0
     */
    fun removeListener(dataId: String?, group: String?, listener: AbstractConfigChangeListener?) {
        configService.removeListener(dataId, group, listener)
    }

    /**
     * Nacos 启动所需 [Properties] 的链式构建器。
     * 单独抽出来是为了让构造调用方避免直接操作可变 [Properties] —— 一次性 put 多个键比传 raw map 更可读。
     *
     * @author K
     * @since 1.0.0
     */
    class PropertiesBuilder {
        /** 累积的属性集合 */
        private val properties = Properties()

        /**
         * 写入一对配置。
         *
         * @param key 属性名（一般是 Nacos SDK 已定义的常量）
         * @param value 属性值
         * @return 当前 builder 自身，便于链式调用
         * @author K
         * @since 1.0.0
         */
        fun put(key: Any?, value: Any?): PropertiesBuilder = apply { properties[key] = value }

        /**
         * @return 累积好的 [Properties]
         * @author K
         * @since 1.0.0
         */
        fun get(): Properties = properties
    }

    companion object {
        /** Nacos server 地址属性名 */
        const val PRO_SERVER_ADDR_KEY: String = "serverAddr"

        /**
         * 按 `(serverAddr, namespace)` 分桶缓存 [ConfigService]。
         *
         * 进程级 + 并发安全：`ConfigService` 是重型对象，但同集群应当复用一份；
         * 不同集群必须独立，否则 SDK 内部连接会错乱。`computeIfAbsent` 保证同 key
         * 只 new 一次。
         */
        private val SERVICE_CACHE = ConcurrentHashMap<CacheKey, ConfigService>()

        /**
         * 按 properties 派生的缓存 key 取或创建 [ConfigService]。
         *
         * `computeIfAbsent` + 包装 [NacosException] 为 RuntimeException 让初始化期失败
         * 直接打断启动而非静默吞掉。
         *
         * @param properties Nacos SDK 启动参数
         * @return 已缓存或新建的 [ConfigService]
         * @author K
         * @since 1.0.0
         */
        private fun obtainConfigService(properties: Properties): ConfigService {
            val key = CacheKey.of(properties)
            // computeIfAbsent + putIfAbsent 都行；前者更直观，且 NacosFactory.createConfigService
            // 内部已经做过自己的初始化串行化，本处只需 map-level 串行
            return SERVICE_CACHE.computeIfAbsent(key) {
                try {
                    NacosFactory.createConfigService(properties)
                } catch (e: NacosException) {
                    throw RuntimeException(e)
                }
            }
        }

        /**
         * 缓存 key——只取 nacos SDK 用来识别 "哪个集群 / 哪个 namespace" 的两个关键属性，
         * 避免把 password / accessKey 等敏感信息也纳入 key（也避免 properties 顺序敏感性问题）。
         */
        private data class CacheKey(val serverAddr: String, val namespace: String) {
            companion object {
                /**
                 * 从 [Properties] 提取 serverAddr + namespace 构造 [CacheKey]。
                 *
                 * @param properties Nacos SDK 配置
                 * @return key 实例
                 * @author K
                 * @since 1.0.0
                 */
                fun of(properties: Properties): CacheKey = CacheKey(
                    serverAddr = properties.getProperty(PRO_SERVER_ADDR_KEY).orEmpty(),
                    namespace = properties.getProperty(PropertyKeyConst.NAMESPACE).orEmpty(),
                )
            }
        }
    }
}
