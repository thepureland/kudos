package io.kudos.ability.distributed.stream.common.biz

import io.kudos.ability.data.rdb.ktorm.biz.BaseCrudBiz
import io.kudos.ability.distributed.stream.common.dao.StreamExceptionMsgDao
import io.kudos.ability.distributed.stream.common.model.po.SysMqFailMsg
import io.kudos.ability.distributed.stream.common.model.table.SysMqFailMsgs
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * @Description stream异常消息处理
 * @Author paul
 * @Date 2022/10/19 16:06
 */
open class SysMqFailMsgBiz :
    BaseCrudBiz<String, SysMqFailMsg, StreamExceptionMsgDao>(),
    ISysMqFailMsgBiz {

    @Autowired
    private lateinit var streamExceptionMsgDao: StreamExceptionMsgDao

    /**
     * 保存异常消息
     *
     * @param exceptionMsg
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun save(exceptionMsg: SysMqFailMsg): Boolean {
        streamExceptionMsgDao.insert(exceptionMsg)
        return true
    }

    /**
     * 查询指定topic下的异常消息
     *
     * @param topic     主题
     * @param startTime 查询开始时间
     */
    override fun query(topic: String, startTime: LocalDateTime): List<SysMqFailMsg> {
        val criteria = Criteria(SysMqFailMsgs.topic.name, OperatorEnum.EQ, topic)
            .addAnd(SysMqFailMsgs.createTime.name, OperatorEnum.GE, startTime)
        return streamExceptionMsgDao.search(criteria)
    }

    /**
     * 删除异常消息
     *
     * @param ids
     */
    @Transactional(rollbackFor = [Exception::class])
    override fun delete(ids: List<String>) {
        val count = streamExceptionMsgDao.batchDelete(ids)
        LOG.info("删除stream异常消息条数:{0}", count)
    }

    private val LOG = LogFactory.getLog(this)

}
