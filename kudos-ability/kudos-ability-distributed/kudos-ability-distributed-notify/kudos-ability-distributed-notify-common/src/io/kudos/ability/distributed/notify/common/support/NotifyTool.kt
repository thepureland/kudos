package io.kudos.ability.distributed.notify.common.support

import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.init.properties.NotifyCommonProperties
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.base.logger.LogFactory
import java.io.Serializable

/**
 * Notification utility.
 *
 * Provides a unified facade for sending notifications so callers do not depend directly on a concrete
 * [INotifyProducer] implementation. When `notifyProducer` is injected as null, behavior is governed by
 * [NotifyCommonProperties.failOnMissingProducer]: throw an error, or warn and return false—letting apps
 * skip the MQ dependency in dev while requiring a producer in production.
 *
 * @author K
 * @since 1.0.0
 */
class NotifyTool(
    /** Optional notification producer implementation; when absent, behavior is governed by configuration (degrade or throw). */
    private val notifyProducer: INotifyProducer?,
    /** Common notification configuration; exposes switches such as failOnMissingProducer. */
    private val properties: NotifyCommonProperties
) {

    /** Logger. */
    private val log = LogFactory.getLog(this::class)

    /**
     * Sends a notification message.
     *
     * If a producer is present, deliver directly; otherwise behavior follows [NotifyCommonProperties.failOnMissingProducer]:
     * - true -> throw [IllegalStateException]
     * - false -> log a WARN and return false
     *
     * @param messageVo the notification payload
     * @return whether delivery succeeded
     * @throws IllegalStateException when fail-on-missing-producer = true and the producer is absent
     * @author K
     * @since 1.0.0
     */
    fun notify(messageVo: NotifyMessageVo<out Serializable>): Boolean {
        notifyProducer?.let { return it.notify(messageVo) }
        val msg = "No INotifyProducer implementation registered"
        if (properties.failOnMissingProducer) {
            throw IllegalStateException(msg)
        }
        log.warn(msg)
        return false
    }

}
