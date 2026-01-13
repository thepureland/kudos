package io.kudos.ability.distributed.stream.common.handler

import io.kudos.ability.distributed.stream.common.model.vo.StreamMessageVo
import io.kudos.ability.distributed.stream.common.model.vo.StreamProducerMsgVo
import io.kudos.ability.distributed.stream.common.support.StreamProducerHelper
import io.kudos.base.data.json.JsonKit
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.retry.AbstractFailedDataHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.support.GenericMessage

/**
 * 流式消息生产者异常处理器
 * 处理消息发送失败的情况，支持将失败消息持久化到本地文件，并定时重试发送
 */
class StreamProducerExceptionHandler : AbstractFailedDataHandler<StreamProducerMsgVo>(), IStreamFailHandler {

    @Value("\${kudos.ability.distributed.stream.produce-fail-path:/var/data/failed}")
    private val filePath = "/var/data/failed"

    @Autowired
    private lateinit var streamProducerHelper: StreamProducerHelper

    protected override fun processFailedData(data: StreamProducerMsgVo): Boolean {
        val bindName = data.bindName
        val msgBodyJson = data.msgBodyJson
        val msgHeaderJson = data.msgHeaderJson
        val obj = JsonKit.fromJson<Any>(msgBodyJson!!)
        val streamMessageVo: StreamMessageVo<Any?> = StreamMessageVo(obj)
        val headMap = JsonKit.fromJson<MutableMap<String, Any>>(msgHeaderJson!!)
        val messageHeaders = MessageHeaders(headMap!!)
        val message = GenericMessage(streamMessageVo, messageHeaders)
        //如果本次发送为异步flush报错，则它会继续被异常拦截
        //如果本次直接返回false，它则没提交到mq的等待flush队列中，本地文件不做删除
        return streamProducerHelper.doRealSend(bindName!!, message, true)
    }

    override val businessType: String
        get() = "mq-producer-fail"

    override val cronExpression: String
        get() = "0 0/1 * * * *"

    override fun filePath(): String {
        return filePath + "/" + KudosContextHolder.get().atomicServiceCode
    }

    override fun bindName(): String {
        return IStreamFailHandler.Companion.DEFAULT_BIND_NAME
    }

}
