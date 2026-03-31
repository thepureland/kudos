package io.kudos.ms.msg.common.vo.receivergroup.request


/**
 * 消息接收者群组表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiverGroupFormCreate (

    override val receiverGroupTypeDictCode: String? = null,

    override val defineTable: String? = null,

    override val nameColumn: String? = null,

    override val remark: String? = null,

    override val active: Boolean? = null,

) : IMsgReceiverGroupFormBase
