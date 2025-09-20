package io.kudos.ability.log.audit.commobn.entity

import io.kudos.ability.log.audit.commobn.annotation.Audit
import io.kudos.ability.log.audit.commobn.annotation.WebAudit
import io.kudos.ability.log.audit.commobn.support.ILogVo


/**
 * Create by (admin) on 7/9/15.
 * 主日志
 */
class LogVo : ILogVo {

    /**
     * 审计日志
     *
     * @author admin
     */
    var logs: MutableList<BaseLog?>? = null
        private set

    init {
        newLogs()
    }

    /**
     * 添加系统级日志
     *
     * @author admin
     */
    fun addAuditLog(audit: WebAudit): BaseLog {
        val sysLog = BaseLog(audit)
        this.logs!!.add(sysLog)
        return sysLog
    }

    /**
     * 添加系统级日志
     *
     * @author admin
     */
    fun addAuditLog(audit: Audit): BaseLog {
        val sysLog = BaseLog(audit)
        this.logs!!.add(sysLog)
        return sysLog
    }

    private fun newLogs() {
        if (this.logs == null) {
            logs = ArrayList<BaseLog?>(2)
        }
    }

    companion object {
        private val serialVersionUID = -6940790149742441845L
    }
}
