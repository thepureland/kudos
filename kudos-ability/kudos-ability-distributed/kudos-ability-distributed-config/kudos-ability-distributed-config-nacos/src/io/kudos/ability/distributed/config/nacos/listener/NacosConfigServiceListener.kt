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

    @Throws(NacosException::class)
    fun addListener(dataId: String?, group: String?, listener: AbstractConfigChangeListener?) {
        configService.addListener(dataId, group, listener)
    }

    fun removeListener(dataId: String?, group: String?, listener: AbstractConfigChangeListener?) {
        configService.removeListener(dataId, group, listener)
    }

    class PropertiesBuilder {
        private val properties = Properties()

        fun put(key: Any?, value: Any?): PropertiesBuilder {
            this.properties[key] = value
            return this
        }

        fun get(): Properties = properties
    }

    companion object {
        const val PRO_SERVER_ADDR_KEY: String = "serverAddr"

        /**
         * 按 `(serverAddr, namespace)` 分桶缓存 [ConfigService]。
         *
         * 进程级 + 并发安全：`ConfigService` 是重型对象，但同集群应当复用一份；
         * 不同集群必须独立，否则 SDK 内部连接会错乱。`computeIfAbsent` 保证同 key
         * 只 new 一次。
         */
        private val SERVICE_CACHE = ConcurrentHashMap<CacheKey, ConfigService>()

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
                fun of(properties: Properties): CacheKey = CacheKey(
                    serverAddr = properties.getProperty(PRO_SERVER_ADDR_KEY).orEmpty(),
                    namespace = properties.getProperty(PropertyKeyConst.NAMESPACE).orEmpty(),
                )
            }
        }
    }
}
