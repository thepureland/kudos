package io.kudos.ability.log.audit.common.entity

import io.kudos.ability.log.audit.common.annotation.Audit
import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.support.ILogVo


/**
 * 主审计日志载体——一个方法调用可以产生 N 条 [BaseLog]（不同子系统 / 模块的关联记录）。
 *
 * @author K
 * @since 1.0.0
 */
class LogVo : ILogVo {

    /** 当前方法调用累积的审计日志条目。 */
    var logs = mutableListOf<BaseLog>()

    /** 追加一条由 [WebAudit] 注解派生的日志条目。 */
    fun addAuditLog(audit: WebAudit): BaseLog {
        val sysLog = BaseLog(audit)
        this.logs.add(sysLog)
        return sysLog
    }

    /** 追加一条由 [Audit] 注解派生的日志条目。 */
    fun addAuditLog(audit: Audit): BaseLog {
        val sysLog = BaseLog(audit)
        this.logs.add(sysLog)
        return sysLog
    }

    companion object {
        private const val serialVersionUID = -6940790149742441845L
    }
}
