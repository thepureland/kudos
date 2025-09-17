package io.kudos.ability.distributed.config.nacos

import com.alibaba.cloud.nacos.NacosPropertySourceRepository
import io.kudos.context.config.IConfigDataFinder
import org.springframework.core.env.PropertySource


/**
 *
 * @author hanson
 * @since 1.0.0
 */
class NacosConfigDataFinder : IConfigDataFinder {
    override fun findConfigData(name: String?): PropertySource<*>? {
        val nacosPropertySources = NacosPropertySourceRepository.getAll()
        for (nacosPropertySource in nacosPropertySources) {
            if (nacosPropertySource.dataId == name) {
                return nacosPropertySource
            }
        }
        return null
    }
}
