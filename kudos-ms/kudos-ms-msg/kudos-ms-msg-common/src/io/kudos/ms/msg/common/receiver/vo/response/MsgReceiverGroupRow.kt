package io.kudos.ms.msg.common.receiver.vo.response

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * List query result row VO for the message receiver group.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiverGroupRow (

    /** Primary key */
    override val id: String = "",

    /** Receiver group type dictionary code */
    val receiverGroupTypeDictCode: String? = null,

    /** Table where the group is defined */
    val defineTable: String? = null,

    /** Column name of the group name within the specific group table */
    val nameColumn: String? = null,

    /** Remark */
    val remark: String? = null,

    /** Whether enabled */
    val active: Boolean? = null,

    /** Whether built-in */
    val builtIn: Boolean? = null,

) : IIdEntity<String>