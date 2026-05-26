package io.kudos.ability.log.audit.common.api

import io.kudos.ability.log.audit.common.entity.SysMonitorMsgVo

/**
 * Monitor-message producer interface: a peer of [IAuditService], dedicated to "exception/alert" messages (distinct
 * from business-operation auditing).
 *
 * [MonitorMsgTool.pushErrMsg] is the most common reporting entry point; the implementation chooses whether `submit`
 * goes over DB / MQ / stdout, etc.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMonitorService {
    /**
     * Audit-log producer method
     *
     * @param monitorMsgVo
     */
    fun submit(monitorMsgVo: SysMonitorMsgVo): Boolean
}
