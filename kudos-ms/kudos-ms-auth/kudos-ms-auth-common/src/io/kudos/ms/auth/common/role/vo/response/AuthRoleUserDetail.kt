package io.kudos.ms.auth.common.role.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Response VO for role-user relation detail.
 *
 * @author K
 * @since 1.0.0
 */
data class AuthRoleUserDetail (

    /** Primary key. */
    override val id: String = "",

    /** Role id. */
    val roleId: String? = null,

    /** User id. */
    val userId: String? = null,

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