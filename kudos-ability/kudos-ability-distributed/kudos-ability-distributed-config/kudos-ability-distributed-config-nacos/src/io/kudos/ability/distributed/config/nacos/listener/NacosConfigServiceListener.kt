package io.kudos.ability.distributed.config.nacos.listener

import com.alibaba.nacos.api.NacosFactory
import com.alibaba.nacos.api.PropertyKeyConst
import com.alibaba.nacos.api.config.ConfigService
import com.alibaba.nacos.api.exception.NacosException
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

/**
 * Nacos config file listener — a thin wrapper around `ConfigService` exposing the two most common
 * operations, `addListener` and `removeListener`, to application code.
 *
 * **Multi-cluster support**: [ConfigService] is cached in a process-level map keyed by
 * `(serverAddr, namespace)` (see [SERVICE_CACHE]); different Nacos clusters can coexist. The same
 * `(serverAddr, namespace)` pair still reuses one heavy object, avoiding duplicate HTTP / gRPC
 * client + scheduler thread overhead inside the SDK.
 *
 * Historical context: the previous implementation made `configService` a `@Volatile var`
 * singleton with double-checked init — the first set of properties won, later constructions were
 * silently ignored, and running two different Nacos clusters in the same process was simply
 * broken. This fix switches to bucketed caching.
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
     * Register a config-change listener for a dataId + group pair.
     *
     * @param dataId Nacos dataId
     * @param group Nacos group
     * @param listener the abstract listener provided by Nacos (application code only needs to implement onChange)
     * @throws NacosException thrown by the Nacos SDK
     * @author K
     * @since 1.0.0
     */
    @Throws(NacosException::class)
    fun addListener(dataId: String?, group: String?, listener: AbstractConfigChangeListener?) {
        configService.addListener(dataId, group, listener)
    }

    /**
     * Unregister a listener previously registered via [addListener].
     *
     * @param dataId Nacos dataId
     * @param group Nacos group
     * @param listener the listener instance to remove (must be the same reference used at registration)
     * @author K
     * @since 1.0.0
     */
    fun removeListener(dataId: String?, group: String?, listener: AbstractConfigChangeListener?) {
        configService.removeListener(dataId, group, listener)
    }

    /**
     * Chained builder for the [Properties] required to start Nacos.
     * Extracted so that callers avoid mutating a raw [Properties] directly — putting several keys
     * in a fluent chain is more readable than passing a raw map.
     *
     * @author K
     * @since 1.0.0
     */
    class PropertiesBuilder {
        /** Accumulated properties. */
        private val properties = Properties()

        /**
         * Write a single key-value pair.
         *
         * @param key property name (typically a Nacos SDK constant)
         * @param value property value
         * @return the builder itself, for chaining
         * @author K
         * @since 1.0.0
         */
        fun put(key: Any?, value: Any?): PropertiesBuilder = apply { properties[key] = value }

        /**
         * @return the accumulated [Properties]
         * @author K
         * @since 1.0.0
         */
        fun get(): Properties = properties
    }

    companion object {
        /** Property name for the Nacos server address. */
        const val PRO_SERVER_ADDR_KEY: String = "serverAddr"

        /**
         * Bucketed cache of [ConfigService] keyed by `(serverAddr, namespace)`.
         *
         * Process-level and thread-safe: `ConfigService` is a heavy object that should be reused
         * per cluster, but different clusters must stay independent or the SDK's internal
         * connections get mixed up. `computeIfAbsent` guarantees a single new instance per key.
         */
        private val SERVICE_CACHE = ConcurrentHashMap<CacheKey, ConfigService>()

        /**
         * Get or create a [ConfigService] using the cache key derived from the properties.
         *
         * `computeIfAbsent` plus wrapping [NacosException] as RuntimeException ensures that
         * init-time failures interrupt startup rather than being silently swallowed.
         *
         * @param properties Nacos SDK startup parameters
         * @return the cached or newly created [ConfigService]
         * @author K
         * @since 1.0.0
         */
        private fun obtainConfigService(properties: Properties): ConfigService {
            val key = CacheKey.of(properties)
            // Either computeIfAbsent or putIfAbsent works; the former is clearer, and since
            // NacosFactory.createConfigService already serialises its own initialisation we only
            // need map-level serialisation here.
            return SERVICE_CACHE.computeIfAbsent(key) {
                try {
                    NacosFactory.createConfigService(properties)
                } catch (e: NacosException) {
                    throw RuntimeException(e)
                }
            }
        }

        /**
         * Cache key — keeps only the two properties the Nacos SDK uses to identify
         * "which cluster / which namespace", so sensitive fields like password / accessKey are
         * not folded into the key (also avoiding ordering sensitivity of `Properties`).
         */
        private data class CacheKey(val serverAddr: String, val namespace: String) {
            companion object {
                /**
                 * Build a [CacheKey] by extracting serverAddr + namespace from [Properties].
                 *
                 * @param properties Nacos SDK configuration
                 * @return the key instance
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
