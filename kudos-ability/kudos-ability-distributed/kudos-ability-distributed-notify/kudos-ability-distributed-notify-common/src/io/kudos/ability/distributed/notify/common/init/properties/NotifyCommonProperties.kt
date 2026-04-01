package io.kudos.ability.distributed.notify.common.init.properties

/**
 * notify 公共配置。
 *
 * 绑定前缀：`kudos.ability.distributed.notify`
 */
open class NotifyCommonProperties {

    /**
     * 为 true 时，如果未装配 [io.kudos.ability.distributed.notify.common.api.INotifyProducer]，
     * 则在启动期直接失败（fail-fast）。
     */
    var failOnMissingProducer: Boolean = false

    /**
     * listener 命名空间。为空时，默认使用 `spring.application.name`。
     */
    var listenerNamespace: String? = null
}

