package io.kudos.ability.log.audit.mq.beans

import io.kudos.ability.distributed.stream.common.annotations.MqProducer
import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.entity.SysAuditLogModel


/**
 * 把审计日志发到 MQ 的 [IAuditService] 实现。
 *
 * **关键设计**：[submit] 方法**自身什么都不做**——返回 `true` 是占位。真正的 MQ 发送由
 * [MqProducer] 注解触发的 AOP 切面拦截参数完成；切面会按 `topic` + `bindingName` 路由
 * 到 spring-cloud-stream 的对应 binding（见 `kudos-ability-log-audit-mq.yml` 的
 * `logAudit-out-0`）。
 *
 * **如果应用没引入 `kudos-ability-distributed-stream-*` 的 MQ producer 切面，本方法实际是
 * no-op**——审计日志静默丢失。所以 MQ 落地要求：
 *  - 装 `kudos-ability-distributed-stream-mq-rabbit` 等 stream 实现模块
 *  - 在 yml 配好 `spring.cloud.stream.bindings.logAudit-out-0.destination`
 *
 * @author K
 * @since 1.0.0
 */
class MqAuditService : IAuditService {

    @MqProducer(topic = "LOG_AUDIT_TOPIC", bindingName = "logAudit-out-0")
    override fun submit(sysAuditLogVo: SysAuditLogModel): Boolean = true

}
