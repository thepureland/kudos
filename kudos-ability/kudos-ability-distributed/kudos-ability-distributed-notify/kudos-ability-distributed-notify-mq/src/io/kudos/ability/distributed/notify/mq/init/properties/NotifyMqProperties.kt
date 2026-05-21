package io.kudos.ability.distributed.notify.mq.init.properties

/**
 * notify-mq 扩展配置。
 *
 * 绑定前缀：`kudos.ability.distributed.notify.mq`
 */
open class NotifyMqProperties {

    /**
     * 为 true 时，启动期没有找到生产者 binding 配置会直接失败。
     */
    var failOnMissingProducerBinding: Boolean = false

    /**
     * 消费端处理失败时是否重新抛出异常。
     *
     * 默认 false 保持历史行为：只记录日志并 ack；生产建议打开，让底层 MQ binder 按配置重试或进入 DLQ。
     */
    var rethrowConsumerException: Boolean = false
}
