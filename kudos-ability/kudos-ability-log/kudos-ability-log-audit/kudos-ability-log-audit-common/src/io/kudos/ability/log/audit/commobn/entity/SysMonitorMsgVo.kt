package io.kudos.ability.log.audit.commobn.entity

import java.io.Serializable
import java.util.*

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
        private const val serialVersionUID = 1L
    }
}
