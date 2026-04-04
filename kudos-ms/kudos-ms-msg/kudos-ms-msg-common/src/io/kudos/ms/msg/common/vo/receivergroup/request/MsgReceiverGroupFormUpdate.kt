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
    override val id: String,

    override val receiverGroupTypeDictCode: String?,

    override val defineTable: String?,

    override val nameColumn: String?,

    override val remark: String?,

    override val active: Boolean?,

) : IIdEntity<String>, IMsgReceiverGroupFormBase
