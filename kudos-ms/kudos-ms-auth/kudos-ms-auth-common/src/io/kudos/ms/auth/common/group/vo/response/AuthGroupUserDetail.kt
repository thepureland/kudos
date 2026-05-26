package io.kudos.ms.auth.common.group.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Response VO for group-user relationship detail.
 *
 * @author K
 * @since 1.0.0
 */
data class AuthGroupUserDetail (

    /** Primary key. */
    override val id: String = "",

    /** Group id. */
    val groupId: String? = null,

    /** User id. */
    val userId: String? = null,

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