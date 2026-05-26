package io.kudos.ms.msg.common.receiver.vo.response

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * Message receiver group edit response VO.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiverGroupEdit (

    /** Primary key. */
    override val id: String = "",

    /** Receiver group type dict code. */
    val receiverGroupTypeDictCode: String? = null,

    /** Table where the group is defined. */
    val defineTable: String? = null,

    /** Column name of the group name in the concrete group table. */
    val nameColumn: String? = null,

    /** Remark. */
    val remark: String? = null,

    /** Whether the group is active. */
    val active: Boolean? = null,

) : IIdEntity<String>
