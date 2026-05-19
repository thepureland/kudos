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
 * **已通过 `resources/META-INF/services/io.kudos.context.config.IConfigDataFinder` 注册**——
 * 只要本模块在 classpath 且 spring-cloud-alibaba Nacos 客户端已拉到 PropertySource，
 * `YamlPropertySourceFactory.loadFromConfigCenter` 就会优先用 Nacos 的内容覆盖本地 yml。
 *
 * 想退出该行为可以在业务侧覆写 SPI（更高 ServiceLoader 优先级的 jar）或者干脆排除本模块。
 *
 * @author hanson
 * @author K
 * @since 1.0.0
 */
class NacosConfigDataFinder : IConfigDataFinder {
    override fun findConfigData(name: String?): PropertySource<*>? =
        NacosPropertySourceRepository.getAll().firstOrNull { it.dataId == name }
}
