package io.kudos.ability.log.audit.mq.beans

import io.kudos.ability.distributed.stream.common.annotations.MqProducer
import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.entity.SysAuditLogModel


class MqAuditService : IAuditService {

    @MqProducer(topic = "LOG_AUDIT_TOPIC", bindingName = "logAudit-out-0")
    override fun submit(sysAuditLogVo: SysAuditLogModel): Boolean {
        return true
    }

}
