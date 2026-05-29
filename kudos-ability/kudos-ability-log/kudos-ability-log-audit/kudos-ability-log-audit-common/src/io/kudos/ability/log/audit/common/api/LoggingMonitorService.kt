package io.kudos.ability.log.audit.common.api

import io.kudos.ability.log.audit.common.entity.SysMonitorMsgVo
import io.kudos.base.logger.LogFactory

/**
 * SLF4J-backed default implementation of [IMonitorService].
 *
 * **Why this exists**: [io.kudos.ability.log.audit.common.support.MonitorMsgTool.pushErrMsg] looks up
 * an `IMonitorService` from the Spring container on every call. Without a concrete bean, any caller
 * (typically business-side exception handlers) gets a `NoSuchBeanDefinitionException` at the worst
 * possible time — during an existing error flow. Apps that haven't pulled in a delivery module
 * (e.g. `kudos-ability-log-audit-mq`) need a working default so monitor reports degrade to a
 * structured log line instead of throwing.
 *
 * **Behavior**: emits an ERROR-level log carrying every populated field on the [SysMonitorMsgVo]
 * (tenant / application / exception type / call source / time / message). Always returns `true`
 * (logging is best-effort; a write failure inside slf4j is silently swallowed by the framework, so
 * we don't surface partial-success to the caller — that would suggest a recovery option that does
 * not exist here).
 *
 * **When NOT to use**: when the app needs durable monitor delivery (alert pipelines, central
 * dashboards), bring in `kudos-ability-log-audit-mq` or write a custom `IMonitorService` bean —
 * either will replace this fallback via `@ConditionalOnMissingBean` in
 * [io.kudos.ability.log.audit.common.starter.LogAuditCommonConfiguration].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class LoggingMonitorService : IMonitorService {

    private val log = LogFactory.getLog(this::class)

    override fun submit(monitorMsgVo: SysMonitorMsgVo): Boolean {
        // Single structured ERROR line so logging backends with JSON output (logback-classic +
        // logstash-encoder, OpenTelemetry sinks) can pivot on `exceptionType` / `tenantId` / etc.
        log.error(
            "[Monitor] tenantId={0} application={1} exceptionType={2} callSource={3} createTime={4} message={5}",
            monitorMsgVo.tenantId.orEmpty(),
            monitorMsgVo.applicationName.orEmpty(),
            monitorMsgVo.exceptionType.orEmpty(),
            monitorMsgVo.callSource.orEmpty(),
            monitorMsgVo.createTime?.toString().orEmpty(),
            monitorMsgVo.exceptionMsg.orEmpty(),
        )
        return true
    }
}
