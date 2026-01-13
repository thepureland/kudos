package io.kudos.ability.distributed.stream.common.handler

import io.kudos.ability.distributed.stream.common.model.vo.StreamProducerMsgVo

/**
 * 流式消息失败处理器接口
 * 用于处理消息发送失败的情况，支持持久化失败数据以便后续重试
 */
interface IStreamFailHandler {
    fun bindName(): String?

    fun persistFailedData(data: StreamProducerMsgVo): String?

    companion object {
        const val DEFAULT_BIND_NAME: String = "_s_default_"
        const val CHANNEL_BEN_NAME: String = "mqProducerChannel"
    }
}
