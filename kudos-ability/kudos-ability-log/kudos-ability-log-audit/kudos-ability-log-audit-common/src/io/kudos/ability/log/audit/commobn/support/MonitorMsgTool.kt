package io.kudos.ability.log.audit.commobn.support

import io.kudos.ability.log.audit.commobn.api.IMonitorService
import io.kudos.ability.log.audit.commobn.entity.SysMonitorMsgVo
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import java.util.*
import java.util.function.Function
import java.util.stream.Stream
import kotlin.math.min

object MonitorMsgTool {
    private val WALKER: StackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)

    fun pushErrMsg(msg: String?, exceptionType: String?, throwable: Throwable?) {
        val callSource: Pair<String?, String?>? = callSource
        val sysMonitorMsgVo = realCreateSysMonitorMsgVo(msg, exceptionType, throwable, callSource)
        SpringKit.getBean(IMonitorService::class).submit(sysMonitorMsgVo)
    }

    fun createSysMonitorMsgVo(msg: String?, exceptionType: String?, throwable: Throwable?): SysMonitorMsgVo {
        val callSource: Pair<String?, String?>? = callSource
        return realCreateSysMonitorMsgVo(msg, exceptionType, throwable, callSource)
    }

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
        sysMonitorMsgVo.exceptionType=exceptionType
        sysMonitorMsgVo.createTime = Date()
        var errorMsg = msg
        if (throwable != null) {
            val topStackTraceLines = getTopStackTraceLines(throwable, 10)
            errorMsg += "\n\n" + topStackTraceLines
        }
        sysMonitorMsgVo.exceptionMsg=errorMsg
        return sysMonitorMsgVo
    }

    private val callSource: Pair<String?, String?>?
        get() {
            val caller =
                WALKER.walk<StackWalker.StackFrame?>(Function { frames: Stream<StackWalker.StackFrame?>? ->
                    frames!!.skip(2)
                        .findFirst()
                        .orElse(null)
                }
                )
            if (caller != null) {
                return Pair(caller.className, caller.methodName)
            }
            return null
        }


    /**
     * 获取 Throwable 的前 n 行堆栈信息
     */
    private fun getTopStackTraceLines(throwable: Throwable, max: Int): String {
        val sb = StringBuilder()
        // 打印异常类型和消息
        sb.append(throwable).append('\n')

        // 直接拿 throwable 自带的 StackTraceElement 数组
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
