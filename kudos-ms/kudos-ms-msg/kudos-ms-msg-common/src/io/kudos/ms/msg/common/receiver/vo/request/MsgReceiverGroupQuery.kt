package io.kudos.ms.msg.common.receiver.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.msg.common.receiver.vo.response.MsgReceiverGroupRow


/**
 * Query criteria request VO for the message receiver group list.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiverGroupQuery (

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

) : ListSearchPayload() {

    override fun getReturnEntityClass() = MsgReceiverGroupRow::class

}