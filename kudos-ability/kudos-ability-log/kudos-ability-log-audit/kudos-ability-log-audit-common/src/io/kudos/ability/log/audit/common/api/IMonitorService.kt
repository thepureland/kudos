package io.kudos.ability.log.audit.common.api

import io.kudos.ability.log.audit.common.entity.SysMonitorMsgVo

interface IMonitorService {
    /**
     * 审计日志生产方法
     *
     * @param monitorMsgVo
     */
    fun submit(monitorMsgVo: SysMonitorMsgVo): Boolean
}
