package io.kudos.ability.log.audit.common.support

import io.kudos.ability.log.audit.common.entity.BaseLog

/**
 * 审计日志详情描述格式化器：业务侧实现该接口可定制每条审计日志在详情页的展示文本。
 *
 * 工作流：
 * 1. [needFormat] 询问是否处理该 [BaseLog]（默认 false，即不接管）
 * 2. 接管时切面会先调 [loadOldBizData] 让业务从入参反查旧数据（用于"变更前/后"对比）
 * 3. 最后 [descriptionFormat] 把 [BaseLog] 翻成展示用字符串
 *
 * 默认实现 (Web 操作日志) 见 [DefaultAuditLogDetailDescriptionFormatter]；
 * 业务自定义实现按 Spring bean 注入由切面自动发现并调用。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IAuditLogDetailDescriptionFormatter {
    /**
     * 是否本业务需要转换(非web操作日志)
     *
     * @param baseLog
     */
    fun needFormat(baseLog: BaseLog?): Boolean {
        return false
    }

    /**
     * 根据方法的参数，获取旧数据
     *
     * @param paramObjs
     * @return
     */
    fun loadOldBizData(baseLog: BaseLog?, paramObjs: Array<Any?>?): Any? {
        return null
    }

    /**
     * 对日志进行转换
     *
     * @param baseLog 日志信息
     * @return 转换为详情的描述信息
     */
    fun descriptionFormat(baseLog: BaseLog?): String?
}
