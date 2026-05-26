package io.kudos.ability.log.audit.common.entity

import java.io.Serializable
import java.util.Date

/**
 * Monitoring/alert message VO.
 *
 * Used by the business side via [io.kudos.ability.log.audit.common.support.MonitorMsgTool] to send
 * exception information; the monitoring service consumes it and persists it to an alert store or
 * pushes it to an alert channel. Fields focus on "which app, which environment, what exception,
 * where it occurred, when it occurred".
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class SysMonitorMsgVo : Serializable {
    /**
     * Tenant id
     */
    var tenantId: String? = null

    /**
     * Exception type, freely defined by business
     */
    var exceptionType: String? = null

    /**
     * Application name
     */
    var applicationName: String? = null

    /**
     * Exception message
     */
    var exceptionMsg: String? = null

    /**
     * Environment
     */
    var environment: String? = null

    /**
     * Exception source: class path + method name
     */
    var callSource: String? = null

    /**
     * Time the exception was generated
     */
    var createTime: Date? = null

    companion object {
        /** Serializable version uid */
        private const val serialVersionUID = 1L
    }
}
