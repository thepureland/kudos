package io.kudos.ability.log.audit.common.support

import io.kudos.ability.log.audit.common.api.IMonitorService
import io.kudos.ability.log.audit.common.entity.SysMonitorMsgVo
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import java.util.Date
import kotlin.math.min

/**
 * 全局监控消息（异常/告警）上报工具。
 *
 * 责任：
 * - 推送：[pushErrMsg] 直接调 [IMonitorService.submit] 提交
 * - 构造：[createSysMonitorMsgVo] 只构造不提交（用于 caller 自行批量收集）
 *
 * 自动抓"调用源"——通过 [StackWalker] 跳过自身两帧拿到真实调用方的 className + methodName，
 * 比硬编码 caller 信息更鲁棒（重构、内联不易破坏）。
 *
 * 异常堆栈按前 10 行截断，避免单条监控消息体过大压垮 MQ 或日志存储。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object MonitorMsgTool {
    /** StackWalker 实例；启用 RETAIN_CLASS_REFERENCE 后才能拿到 Class 引用（用于 caller 元信息）。 */
    private val WALKER: StackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)

    /**
     * 上报错误消息：构造 [SysMonitorMsgVo] 后通过 Spring 容器找到 [IMonitorService] 提交。
     *
     * @param msg 错误描述
     * @param exceptionType 异常类型标识（自定义错误码或 java 异常 simpleName）
     * @param throwable 异常对象，附带前 10 帧堆栈
     * @author K
     * @since 1.0.0
     */
    fun pushErrMsg(msg: String?, exceptionType: String?, throwable: Throwable?) {
        val callSource: Pair<String?, String?>? = callSource
        val sysMonitorMsgVo = realCreateSysMonitorMsgVo(msg, exceptionType, throwable, callSource)
        SpringKit.getBean<IMonitorService>().submit(sysMonitorMsgVo)
    }

    /**
     * 构造（不提交）监控消息 VO：caller 自行决定何时调用 [IMonitorService.submit]。
     * 典型用途是批量收集后统一上报，减少 MQ 调用次数。
     *
     * @param msg 错误描述
     * @param exceptionType 异常类型标识
     * @param throwable 异常对象
     * @return 待提交的 VO
     * @author K
     * @since 1.0.0
     */
    fun createSysMonitorMsgVo(msg: String?, exceptionType: String?, throwable: Throwable?): SysMonitorMsgVo {
        val callSource: Pair<String?, String?>? = callSource
        return realCreateSysMonitorMsgVo(msg, exceptionType, throwable, callSource)
    }

    /**
     * 真实构造逻辑：填租户/服务/异常类型/时间，可选附带前 10 帧堆栈摘要。
     *
     * @param msg 错误描述
     * @param exceptionType 异常类型标识
     * @param throwable 异常对象；非空时把堆栈摘要拼到 message 末尾
     * @param callSource 调用源 `(className, methodName)`；由 [callSource] 计算
     * @return 完整的 VO
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
     * 通过 StackWalker 跳过自身两帧拿真实调用方信息。
     * "两帧"对应：本属性自身 + [createSysMonitorMsgVo] / [pushErrMsg]。
     *
     * @return `(className, methodName)` 对；无法定位时返回 null
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
