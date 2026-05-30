package io.kudos.ms.auth.common.role.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Response VO for role detail.
 *
 * @author K
 * @since 1.0.0
 */
data class AuthRoleDetail (

    /** Primary key. */
    override val id: String = "",

    /** Role code. */
    val code: String? = null,

    /** Role name. */
    val name: String? = null,

    /** Tenant id. */
    val tenantId: String? = null,

    /** Subsystem code. */
    val subsysCode: String? = null,

    /** Parent role id; NULL = root role. */
    val parentId: String? = null,

    /** Remark. */
    val remark: String? = null,

    /** Whether the role is active. */
    val active: Boolean? = null,

    /** Whether the role is built-in. */
    val builtIn: Boolean? = null,

    /** Creator user id. */
    val createUserId: String? = null,

    /** Creator user name. */
    val createUserName: String? = null,

    /** Creation time. */
    val createTime: LocalDateTime? = null,

    /** Updater user id. */
    val updateUserId: String? = null,

    /** Updater user name. */
    val updateUserName: String? = null,

    /** Last update time. */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>