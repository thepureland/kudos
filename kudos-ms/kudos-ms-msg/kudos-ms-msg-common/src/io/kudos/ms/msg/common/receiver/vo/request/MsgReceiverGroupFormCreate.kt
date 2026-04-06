package io.kudos.ms.msg.common.receiver.vo.request
/**
 * 消息接收者群组表单新建请求VO
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
