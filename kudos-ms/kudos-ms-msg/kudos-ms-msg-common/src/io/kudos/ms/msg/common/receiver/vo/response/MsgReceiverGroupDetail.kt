package io.kudos.ms.msg.common.receiver.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Message receiver group detail response VO.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiverGroupDetail (

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

    /** Active flag. */
    val active: Boolean? = null,

    /** Built-in flag. */
    val builtIn: Boolean? = null,

    /** Creator id. */
    val createUserId: String? = null,

    /** Creator name. */
    val createUserName: String? = null,

    /** Create time. */
    val createTime: LocalDateTime? = null,

    /** Updater id. */
    val updateUserId: String? = null,

    /** Updater name. */
    val updateUserName: String? = null,

    /** Update time. */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>