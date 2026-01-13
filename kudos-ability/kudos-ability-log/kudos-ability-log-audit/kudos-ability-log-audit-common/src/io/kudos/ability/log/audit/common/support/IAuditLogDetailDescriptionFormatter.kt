package io.kudos.ability.log.audit.common.support

import io.kudos.ability.log.audit.common.entity.BaseLog

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
