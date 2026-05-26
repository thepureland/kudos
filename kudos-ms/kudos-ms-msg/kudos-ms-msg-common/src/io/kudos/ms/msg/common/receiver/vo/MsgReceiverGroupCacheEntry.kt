package io.kudos.ms.msg.common.receiver.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * Message receiver group cache entry.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiverGroupCacheEntry (

    /** Primary key. */
    override val id: String,

    /** Receiver group type dict code. */
    val receiverGroupTypeDictCode: String?,

    /** Table where the group is defined. */
    val defineTable: String?,

    /** Column name of the group name in the concrete group table. */
    val nameColumn: String?,

    /** Remark. */
    val remark: String?,

    /** Active flag. */
    val active: Boolean?,

    /** Built-in flag. */
    val builtIn: Boolean?,

    /** Creator id. */
    val createUserId: String?,

    /** Creator name. */
    val createUserName: String?,

    /** Create time. */
    val createTime: LocalDateTime?,

    /** Updater id. */
    val updateUserId: String?,

    /** Updater name. */
    val updateUserName: String?,

    /** Update time. */
    val updateTime: LocalDateTime?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 4762236923181019117L
    }

}
