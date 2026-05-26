package io.kudos.ability.distributed.stream.common.handler

import io.kudos.ability.distributed.stream.common.model.vo.StreamProducerMsgVo

/**
 * Stream message failure handler interface.
 * Handles message send failures and supports persisting failed data for later retry.
 */
interface IStreamFailHandler {
    fun bindName(): String?

    fun persistFailedData(data: StreamProducerMsgVo): String?

    companion object {
        const val DEFAULT_BIND_NAME: String = "_s_default_"
        const val CHANNEL_BEN_NAME: String = "mqProducerChannel"
    }
}
