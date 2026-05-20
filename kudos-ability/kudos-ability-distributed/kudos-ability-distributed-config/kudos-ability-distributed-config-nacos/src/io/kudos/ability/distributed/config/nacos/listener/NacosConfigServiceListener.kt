package io.kudos.ability.distributed.config.nacos.listener

import com.alibaba.nacos.api.NacosFactory
import com.alibaba.nacos.api.config.ConfigService
import com.alibaba.nacos.api.exception.NacosException
import java.util.Properties
import kotlin.concurrent.Volatile

/**
 * Nacos 配置文件监听器——`ConfigService` 的轻封装，给业务侧 `addListener` /
 * `removeListener` 两个最常用操作。
 *
 * **重要：[configService] 是进程级共享单例**。第一个被实例化的 `NacosConfigServiceListener`
 * 创建 `ConfigService`，后续实例的 `serverAddr` / Properties 会被**忽略**——所以同一个进程
 * 内不能给两个不同的 Nacos 集群挂监听。需要时业务侧应自行管理多 `ConfigService` 实例。
 *
 * @author hanson
 * @author K
 * @since 1.0.0
 */
class NacosConfigServiceListener {
    constructor(serverAddr: String?) {
        require(!serverAddr.isNullOrBlank()) { "serverAddr must not be blank" }
        val propertiesBuilder = PropertiesBuilder()
        propertiesBuilder.put(PRO_SERVER_ADDR_KEY, serverAddr)
        initConfigService(propertiesBuilder)
    }

    constructor(propertiesBuilder: PropertiesBuilder) {
        initConfigService(propertiesBuilder)
    }

    /**
     * 初始化进程级共享 [configService]——双重检查 + 类锁，避免多线程并发实例化两份重型对象。
     * **首次初始化的 properties 胜出**，后续调用静默跳过（即便 properties 不同）。
     */
    private fun initConfigService(propertiesBuilder: PropertiesBuilder) {
        if (configService != null) return
        synchronized(NacosConfigServiceListener::class.java) {
            if (configService != null) return
            configService = try {
                NacosFactory.createConfigService(propertiesBuilder.get())
            } catch (e: NacosException) {
                throw RuntimeException(e)
            }
        }
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
        requireNotNull(configService) { "Nacos ConfigService not initialized" }.addListener(dataId, group, listener)
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
        requireNotNull(configService) { "Nacos ConfigService not initialized" }.removeListener(dataId, group, listener)
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
        fun put(key: Any?, value: Any?): PropertiesBuilder {
            this.properties[key] = value
            return this
        }

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

        /** 进程级共享的 [ConfigService]；标 volatile 配合双重检查锁保证可见性 */
        @Volatile
        private var configService: ConfigService? = null
    }
}
