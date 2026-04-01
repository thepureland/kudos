package io.kudos.ability.distributed.stream.common.init.properties

/**
 * Stream 绑定自检配置。
 *
 * 绑定前缀：`kudos.ability.distributed.stream.binding-verify`
 */
open class StreamBindingVerifyProperties {

    /**
     * 是否启用生产者 binding 自检。
     */
    var enabled: Boolean = false

    /**
     * 缺失必需 binding 时是否启动失败（fail-fast）。
     */
    var failOnMissing: Boolean = false

    /**
     * 必需的生产者 binding 名称列表。
     */
    var requiredProducerBindings: List<String> = emptyList()
}

