package io.kudos.ability.distributed.stream.common.biz

import io.kudos.ability.distributed.stream.common.model.po.SysMqFailMsg
import io.kudos.base.support.service.iservice.IBaseCrudService
import java.time.LocalDateTime

/**
 * Stream 异常消息持久化 / 查询 / 清理服务。
 *
 * 配套表 `sys_mq_fail_msg`（[SysMqFailMsg]）；典型调用方：
 *  - [io.kudos.ability.distributed.stream.common.handler.StreamGlobalExceptionHandler.globalHandleError]
 *    consumer 端异常入表
 *  - 业务侧人工补救（按 topic + 时间窗口拉数据 / 重发 / 删除）
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
interface ISysMqFailMsgService : IBaseCrudService<String, SysMqFailMsg> {
    /**
     * 保存异常消息
     *
     * @param exceptionMsg
     */
    fun save(exceptionMsg: SysMqFailMsg): Boolean

    /**
     * 查询指定topic下的异常消息
     *
     * @param topic     主题
     * @param startTime 查询开始时间
     */
    fun query(topic: String, startTime: LocalDateTime): List<SysMqFailMsg>

    /**
     * 删除异常消息
     *
     * @param ids
     */
    fun delete(ids: List<String>)
}