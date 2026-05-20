package io.kudos.ability.distributed.notify.common.support

import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.init.properties.NotifyCommonProperties
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.base.logger.LogFactory
import java.io.Serializable

/**
 * 通知工具类。
 *
 * 提供统一的通知发送门面，让业务侧不直接依赖具体 [INotifyProducer] 实现。
 * `notifyProducer` 注入为 null 时按 [NotifyCommonProperties.failOnMissingProducer] 决定抛错还是 warn 后返回 false——
 * 允许应用在开发环境跳过 MQ 依赖而生产环境强制存在 producer。
 *
 * @author K
 * @since 1.0.0
 */
class NotifyTool(
    /** 可选的通知投递器实现，缺失时按配置决定降级或抛错 */
    private val notifyProducer: INotifyProducer?,
    /** 通知公共配置，提供 failOnMissingProducer 等开关 */
    private val properties: NotifyCommonProperties
) {

    /** 日志器 */
    private val log = LogFactory.getLog(this::class)

    /**
     * 发送通知消息。
     *
     * 有 producer 直接投递；没有时按 [NotifyCommonProperties.failOnMissingProducer] 决定：
     * - true → 抛 [IllegalStateException]
     * - false → 记 WARN 并返回 false
     *
     * @param messageVo 通知载体
     * @return 是否投递成功
     * @throws IllegalStateException 配置 fail-on-missing-producer = true 且 producer 缺失时
     * @author K
     * @since 1.0.0
     */
    fun notify(messageVo: NotifyMessageVo<out Serializable>): Boolean {
        notifyProducer?.let { return it.notify(messageVo) }
        val msg = "未引入 INotifyProducer 实现"
        if (properties.failOnMissingProducer) {
            throw IllegalStateException(msg)
        }
        log.warn(msg)
        return false
    }

}
