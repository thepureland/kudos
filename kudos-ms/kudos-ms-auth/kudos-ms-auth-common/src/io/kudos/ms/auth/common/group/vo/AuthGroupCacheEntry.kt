package io.kudos.ms.auth.common.group.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * User group cache entry.
 *
 * @author K
 * @since 1.0.0
 */
data class AuthGroupCacheEntry (

    /** Primary key. */
    override val id: String,

    /** User group code. */
    val code: String?,

    /** User group name. */
    val name: String?,

    /** Tenant id. */
    val tenantId: String?,

    /** Subsystem code. */
    val subsysCode: String?,

    /** Remark. */
    val remark: String?,

    /** Whether the group is active. */
    val active: Boolean?,

    /** Whether the group is built-in. */
    val builtIn: Boolean?,

    /** Creator id. */
    val createUserId: String?,

    /** Creator name. */
    val createUserName: String?,

    /** Creation time. */
    val createTime: LocalDateTime?,

    /** Updater id. */
    val updateUserId: String?,

    /** Updater name. */
    val updateUserName: String?,

    /** Update time. */
    val updateTime: LocalDateTime?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

}
