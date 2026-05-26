package io.kudos.ability.log.audit.mq.beans

import io.kudos.ability.distributed.stream.common.annotations.MqProducer
import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.entity.SysAuditLogModel


/**
 * [IAuditService] implementation that sends audit logs to MQ.
 *
 * **Key design**: the [submit] method **does nothing by itself** — returning `true` is a placeholder. The actual MQ send
 * is performed by an AOP aspect triggered by the [MqProducer] annotation intercepting the arguments; the aspect routes
 * by `topic` + `bindingName` to the corresponding spring-cloud-stream binding (see `logAudit-out-0` in
 * `kudos-ability-log-audit-mq.yml`).
 *
 * **If the application does not include the MQ producer aspect from `kudos-ability-distributed-stream-*`, this method
 * is effectively a no-op** — audit logs are silently dropped. So requirements for MQ delivery:
 *  - Install a stream implementation module such as `kudos-ability-distributed-stream-mq-rabbit`
 *  - Configure `spring.cloud.stream.bindings.logAudit-out-0.destination` in yml
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class MqAuditService : IAuditService {

    @MqProducer(topic = "LOG_AUDIT_TOPIC", bindingName = "logAudit-out-0")
    override fun submit(sysAuditLogVo: SysAuditLogModel): Boolean = true

}
