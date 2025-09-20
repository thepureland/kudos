package io.kudos.ability.log.audit.commobn.api

import io.kudos.ability.log.audit.commobn.entity.SysMonitorMsgVo

interface IMonitorService {
    /**
     * 审计日志生产方法
     *
     * @param monitorMsgVo
     */
    fun submit(monitorMsgVo: SysMonitorMsgVo): Boolean
}
