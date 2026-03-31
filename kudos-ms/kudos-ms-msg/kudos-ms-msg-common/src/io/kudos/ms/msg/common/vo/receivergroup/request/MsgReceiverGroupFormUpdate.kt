package io.kudos.ms.msg.common.vo.receivergroup.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 消息接收者群组表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiverGroupFormUpdate (

    /** 主键 */
    override val id: String? = null,

    override val receiverGroupTypeDictCode: String? = null,

    override val defineTable: String? = null,

    override val nameColumn: String? = null,

    override val remark: String? = null,

    override val active: Boolean? = null,

) : IIdEntity<String?>, IMsgReceiverGroupFormBase
