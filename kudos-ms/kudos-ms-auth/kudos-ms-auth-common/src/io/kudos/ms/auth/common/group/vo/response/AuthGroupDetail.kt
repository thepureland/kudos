package io.kudos.ms.auth.common.group.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Response VO for user group detail.
 *
 * @author K
 * @since 1.0.0
 */
data class AuthGroupDetail (

    /** Primary key. */
    override val id: String = "",

    /** User group code. */
    val code: String? = null,

    /** User group name. */
    val name: String? = null,

    /** Tenant id. */
    val tenantId: String? = null,

    /** Subsystem code. */
    val subsysCode: String? = null,

    /** Remark. */
    val remark: String? = null,

    /** Whether the group is active. */
    val active: Boolean? = null,

    /** Whether the group is built-in. */
    val builtIn: Boolean? = null,

    /** Creator id. */
    val createUserId: String? = null,

    /** Creator name. */
    val createUserName: String? = null,

    /** Creation time. */
    val createTime: LocalDateTime? = null,

    /** Updater id. */
    val updateUserId: String? = null,

    /** Updater name. */
    val updateUserName: String? = null,

    /** Update time. */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>