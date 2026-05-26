package io.kudos.ability.log.audit.common.support

import io.kudos.ability.log.audit.common.api.IMonitorService
import io.kudos.ability.log.audit.common.entity.SysMonitorMsgVo
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import java.util.Date
import kotlin.math.min

/**
 * Global monitor-message (exception/alert) reporting utility.
 *
 * Responsibilities:
 * - Push: [pushErrMsg] directly calls [IMonitorService.submit] to submit.
 * - Build: [createSysMonitorMsgVo] only builds without submitting (for the caller
 *   to collect in batches themselves).
 *
 * Automatically captures the "call source" — uses [StackWalker] to skip its own
 * two frames and obtain the real caller's className + methodName, which is more
 * robust than hard-coded caller info (refactoring and inlining are less likely
 * to break it).
 *
 * Exception stack traces are truncated to the first 10 lines to avoid a single
 * monitor message becoming too large and overwhelming MQ or log storage.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object MonitorMsgTool {
    /** StackWalker instance; RETAIN_CLASS_REFERENCE must be enabled to obtain Class references (for caller metadata). */
    private val WALKER: StackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)

    /**
     * Reports an error message: builds a [SysMonitorMsgVo] then submits it via
     * [IMonitorService] looked up from the Spring container.
     *
     * @param msg error description
     * @param exceptionType exception type identifier (custom error code or Java exception simpleName)
     * @param throwable exception object, attaches the top 10 stack frames
     * @author K
     * @since 1.0.0
     */
    fun pushErrMsg(msg: String?, exceptionType: String?, throwable: Throwable?) {
        val callSource: Pair<String?, String?>? = callSource
        val sysMonitorMsgVo = realCreateSysMonitorMsgVo(msg, exceptionType, throwable, callSource)
        SpringKit.getBean<IMonitorService>().submit(sysMonitorMsgVo)
    }

    /**
     * Builds (without submitting) a monitor-message VO: the caller decides when to
     * call [IMonitorService.submit]. Typical use is batch collection followed by a
     * single report to reduce MQ call counts.
     *
     * @param msg error description
     * @param exceptionType exception type identifier
     * @param throwable exception object
     * @return the VO to be submitted
     * @author K
     * @since 1.0.0
     */
    fun createSysMonitorMsgVo(msg: String?, exceptionType: String?, throwable: Throwable?): SysMonitorMsgVo {
        val callSource: Pair<String?, String?>? = callSource
        return realCreateSysMonitorMsgVo(msg, exceptionType, throwable, callSource)
    }

    /**
     * Actual build logic: populates tenant / service / exception type / time, and
     * optionally appends the top-10 stack-frame summary.
     *
     * @param msg error description
     * @param exceptionType exception type identifier
     * @param throwable exception object; when non-null, the stack summary is appended to the message
     * @param callSource call source `(className, methodName)` computed by [callSource]
     * @return the complete VO
     * @author K
     * @since 1.0.0
     */
    private fun realCreateSysMonitorMsgVo(
        msg: String?,
        exceptionType: String?,
        throwable: Throwable?,
        callSource: Pair<String?, String?>?
    ): SysMonitorMsgVo {
        val sysMonitorMsgVo = SysMonitorMsgVo()
        sysMonitorMsgVo.callSource = callSource?.toString() ?: ""
        sysMonitorMsgVo.tenantId = KudosContextHolder.get().tenantId
        sysMonitorMsgVo.applicationName = KudosContextHolder.get().atomicServiceCode
        sysMonitorMsgVo.exceptionType = exceptionType
        sysMonitorMsgVo.createTime = Date()
        var errorMsg = msg
        if (throwable != null) {
            val topStackTraceLines = getTopStackTraceLines(throwable, 10)
            errorMsg += "\n\n" + topStackTraceLines
        }
        sysMonitorMsgVo.exceptionMsg = errorMsg
        return sysMonitorMsgVo
    }

    /**
     * Uses StackWalker to skip its own two frames and obtain the real caller info.
     * "Two frames" corresponds to: this property itself + [createSysMonitorMsgVo] /
     * [pushErrMsg].
     *
     * @return `(className, methodName)` pair; null when it cannot be located
     * @author K
     * @since 1.0.0
     */
    private val callSource: Pair<String?, String?>?
        get() {
            val caller = WALKER.walk { s ->
                s.skip(2).findFirst().orElse(null)
            }
            return caller?.let { Pair(it.className, it.methodName) }
        }


    /**
     * Returns the top n stack trace lines of the Throwable.
     */
    private fun getTopStackTraceLines(throwable: Throwable, max: Int): String {
        val sb = StringBuilder()
        // Print the exception type and message.
        sb.append(throwable).append('\n')

        // Directly take the throwable's own StackTraceElement array.
        val frames = throwable.getStackTrace()
        val len = min(frames.size, max)
        for (i in 0..<len) {
            sb.append("\tat ")
                .append(frames[i].toString())
                .append('\n')
        }
        return sb.toString()
    }
}
