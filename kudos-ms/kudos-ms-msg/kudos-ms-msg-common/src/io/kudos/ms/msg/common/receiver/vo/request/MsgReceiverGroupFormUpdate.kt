package io.kudos.ms.msg.common.receiver.vo.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * Update request VO for the message receiver group form.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiverGroupFormUpdate (

    /** Primary key */
    override val id: String,

    override val receiverGroupTypeDictCode: String?,

    override val defineTable: String?,

    override val nameColumn: String?,

    override val remark: String?,

    override val active: Boolean?,

) : IIdEntity<String>, IMsgReceiverGroupFormBase
