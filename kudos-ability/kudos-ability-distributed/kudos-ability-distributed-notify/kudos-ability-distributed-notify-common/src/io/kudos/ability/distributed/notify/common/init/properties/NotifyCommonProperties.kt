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

    /**
     * listener 未命中当前命名空间时，是否继续回落到 default 命名空间。
     *
     * 默认关闭，避免多应用共享同一 MQ topic 且注册相同 notifyType 时误投到 default listener。
     */
    var fallbackToDefaultNamespace: Boolean = false
}
