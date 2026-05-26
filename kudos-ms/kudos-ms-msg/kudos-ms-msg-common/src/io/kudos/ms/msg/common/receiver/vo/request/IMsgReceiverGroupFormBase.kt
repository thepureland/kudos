package io.kudos.ms.msg.common.receiver.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength

/**
 * Base fields of the message receiver group form (shared by create / update).
 *
 * @author K
 * @since 1.0.0
 */
interface IMsgReceiverGroupFormBase {

    /** Receiver group type dictionary code */
    val receiverGroupTypeDictCode: String?

    /** Table where the group is defined */
    val defineTable: String?

    /** Column name of the group name within the specific group table */
    val nameColumn: String?

    /** Remark */
    @get:MaxLength(128)
    val remark: String?

    /** Whether enabled */
    val active: Boolean?
}
