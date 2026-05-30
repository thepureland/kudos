package io.kudos.ms.auth.common.role.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * Cache entry for a role.
 *
 * @author K
 * @since 1.0.0
 */
data class AuthRoleCacheEntry (

    /** Primary key. */
    override val id: String,

    /** Role code. */
    val code: String?,

    /** Role name. */
    val name: String?,

    /** Tenant id. */
    val tenantId: String?,

    /** Subsystem code. */
    val subsysCode: String?,

    /** Parent role id; NULL = root role. */
    val parentId: String? = null,

    /** Remark. */
    val remark: String?,

    /** Whether the role is active. */
    val active: Boolean?,

    /** Whether the role is built-in. */
    val builtIn: Boolean?,

    /** Whether assigning this role requires an approval workflow. */
    val approvalRequired: Boolean? = null,

    /** Creator user id. */
    val createUserId: String?,

    /** Creator user name. */
    val createUserName: String?,

    /** Creation time. */
    val createTime: LocalDateTime?,

    /** Updater user id. */
    val updateUserId: String?,

    /** Updater user name. */
    val updateUserName: String?,

    /** Last update time. */
    val updateTime: LocalDateTime?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

}
