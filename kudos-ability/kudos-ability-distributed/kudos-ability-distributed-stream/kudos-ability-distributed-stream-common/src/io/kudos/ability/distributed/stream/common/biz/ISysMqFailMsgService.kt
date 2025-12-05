package io.kudos.ability.distributed.stream.common.biz

import io.kudos.ability.distributed.stream.common.model.po.SysMqFailMsg
import io.kudos.base.support.iservice.IBaseCrudService
import java.time.LocalDateTime

/**
 * @Description stream异常消息处理接口
 * @Author paul
 * @Date 2022/10/19 16:05
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