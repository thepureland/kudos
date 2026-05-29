package io.kudos.ability.log.audit.mq.beans

import io.kudos.ability.distributed.stream.common.annotations.MqProducer
import io.kudos.ability.log.audit.common.api.IMonitorService
import io.kudos.ability.log.audit.common.entity.SysMonitorMsgVo

/**
 * [IMonitorService] implementation that ships monitor messages to MQ.
 *
 * Mirrors [MqAuditService] precisely: the [submit] body is **a placeholder that returns true**.
 * The actual MQ send is performed by the AOP aspect from `kudos-ability-distributed-stream-common`
 * keyed on the [MqProducer] annotation; it routes by `topic` + `bindingName` to the corresponding
 * spring-cloud-stream binding (see `logMonitor-out-0` in `kudos-ability-log-audit-mq.yml`).
 *
 * **If the application does not include a stream producer aspect from
 * `kudos-ability-distributed-stream-*`, this method is effectively a no-op** — monitor messages are
 * silently dropped. Requirements for actual MQ delivery:
 *  - Install a stream implementation module such as `kudos-ability-distributed-stream-rabbit`
 *  - Configure `spring.cloud.stream.bindings.logMonitor-out-0.destination` in yml
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class MqMonitorService : IMonitorService {

    @MqProducer(topic = "LOG_MONITOR_TOPIC", bindingName = "logMonitor-out-0")
    override fun submit(monitorMsgVo: SysMonitorMsgVo): Boolean = true
}
