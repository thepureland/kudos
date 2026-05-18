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

    @Throws(NacosException::class)
    fun addListener(dataId: String?, group: String?, listener: AbstractConfigChangeListener?) {
        requireNotNull(configService) { "Nacos ConfigService not initialized" }.addListener(dataId, group, listener)
    }

    fun removeListener(dataId: String?, group: String?, listener: AbstractConfigChangeListener?) {
        requireNotNull(configService) { "Nacos ConfigService not initialized" }.removeListener(dataId, group, listener)
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

        @Volatile
        private var configService: ConfigService? = null
    }
}
