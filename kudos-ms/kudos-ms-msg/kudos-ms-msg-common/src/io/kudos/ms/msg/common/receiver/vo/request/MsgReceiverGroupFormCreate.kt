package io.kudos.ms.msg.common.receiver.vo.request

/**
 * Create request VO for the message receiver group form.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiverGroupFormCreate (

    override val receiverGroupTypeDictCode: String? ,

    override val defineTable: String? ,

    override val nameColumn: String? ,

    override val remark: String? ,

    override val active: Boolean? ,

) : IMsgReceiverGroupFormBase
