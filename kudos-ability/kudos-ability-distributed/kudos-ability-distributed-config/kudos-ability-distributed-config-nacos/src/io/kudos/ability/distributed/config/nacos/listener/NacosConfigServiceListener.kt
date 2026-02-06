package io.kudos.ability.distributed.config.nacos.listener

import com.alibaba.nacos.api.NacosFactory
import com.alibaba.nacos.api.config.ConfigService
import com.alibaba.nacos.api.exception.NacosException
import java.util.*
import kotlin.concurrent.Volatile

/**
 * nacos配置文件监听
 *
 * @author hanson
 * @since 1.0.0
 */
class NacosConfigServiceListener {
    constructor(serverAddr: String?) {
        val propertiesBuilder = PropertiesBuilder()
        propertiesBuilder.put(PRO_SERVER_ADDR_KEY, serverAddr)
        initConfigService(propertiesBuilder)
    }

    constructor(propertiesBuilder: PropertiesBuilder) {
        initConfigService(propertiesBuilder)
    }

    private fun initConfigService(propertiesBuilder: PropertiesBuilder) {
        if (configService == null) {
            try {
                configService = NacosFactory.createConfigService(propertiesBuilder.get())
            } catch (e: NacosException) {
                throw RuntimeException(e)
            }
        }
    }

    @Throws(NacosException::class)
    fun addListener(dataId: String?, group: String?, listener: AbstractConfigChangeListener?) {
        configService!!.addListener(dataId, group, listener)
    }

    fun removeListener(dataId: String?, group: String?, listener: AbstractConfigChangeListener?) {
        configService!!.removeListener(dataId, group, listener)
    }

    class PropertiesBuilder {
        private val properties = Properties()

        fun put(key: Any?, value: Any?): PropertiesBuilder {
            this.properties[key] = value
            return this
        }

        fun get(): Properties {
            return this.properties
        }
    }

    companion object {
        const val PRO_SERVER_ADDR_KEY: String = "serverAddr"

        @Volatile
        private var configService: ConfigService? = null
    }
}
