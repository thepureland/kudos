package io.kudos.ability.distributed.config.nacos

import com.alibaba.cloud.nacos.NacosPropertySourceRepository
import io.kudos.context.config.IConfigDataFinder
import org.springframework.core.env.PropertySource

/**
 * 把 Spring Cloud Alibaba 的 [NacosPropertySourceRepository] 适配成 kudos 的 [IConfigDataFinder] SPI。
 *
 * `kudos-context` 的 `YamlPropertySourceFactory` 通过 `ServiceLoader.load(IConfigDataFinder::class.java)`
 * 找配置查询器；本类按 `dataId == name` 在 Nacos 已加载的 PropertySource 中查。
 *
 * **当前未启用**：模块下没有 `META-INF/services/io.kudos.context.config.IConfigDataFinder`
 * 注册文件，所以 ServiceLoader 找不到本类——`NacosPropertySourceRepository.getAll()` 永远不会被调到。
 * 启用方式：在 `resources/META-INF/services/io.kudos.context.config.IConfigDataFinder`
 * 文件里写一行 `io.kudos.ability.distributed.config.nacos.NacosConfigDataFinder`。
 *
 * @author hanson
 * @author K
 * @since 1.0.0
 */
class NacosConfigDataFinder : IConfigDataFinder {
    override fun findConfigData(name: String?): PropertySource<*>? =
        NacosPropertySourceRepository.getAll().firstOrNull { it.dataId == name }
}
