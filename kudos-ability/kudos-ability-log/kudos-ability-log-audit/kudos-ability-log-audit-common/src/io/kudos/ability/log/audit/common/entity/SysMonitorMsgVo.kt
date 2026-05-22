package io.kudos.ability.log.audit.common.entity

import java.io.Serializable
import java.util.Date

/**
 * 监控告警消息 VO。
 *
 * 业务侧通过 [io.kudos.ability.log.audit.common.support.MonitorMsgTool] 投递异常信息时使用本结构，
 * 监控服务侧消费后落入告警库或推送给告警通道。字段聚焦"哪个应用、什么环境、什么异常、何处发生、何时发生"。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class SysMonitorMsgVo : Serializable {
    /**
     * 租户id
     */
    var tenantId: String? = null

    /**
     * 异常类型，业务自由定义
     */
    var exceptionType: String? = null

    /**
     * 应用名
     */
    var applicationName: String? = null

    /**
     * 异常信息
     */
    var exceptionMsg: String? = null

    /**
     * 所属环境
     */
    var environment: String? = null

    /**
     * 异常产生来源：类路径+方法名
     */
    var callSource: String? = null

    /**
     * 异常生成时间
     */
    var createTime: Date? = null

    companion object {
        /** Serializable 版本号 */
        private const val serialVersionUID = 1L
    }
}
