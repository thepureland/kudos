package io.kudos.ability.log.audit.common.api

import io.kudos.ability.log.audit.common.entity.SysMonitorMsgVo

/**
 * 监控消息生产接口：与 [IAuditService] 平级，专门承载"异常/告警"类消息（区别于业务操作审计）。
 *
 * [MonitorMsgTool.pushErrMsg] 是最常用的上报入口；submit 由实现方决定走 DB / MQ / stdout 等通道。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMonitorService {
    /**
     * 审计日志生产方法
     *
     * @param monitorMsgVo
     */
    fun submit(monitorMsgVo: SysMonitorMsgVo): Boolean
}
