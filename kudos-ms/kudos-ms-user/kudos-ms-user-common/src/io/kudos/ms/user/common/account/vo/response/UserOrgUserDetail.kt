package io.kudos.ms.user.common.account.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Organization-user relationship detail response VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserOrgUserDetail (

    /** Primary key */
    override val id: String = "",

    /** Organization id */
    val orgId: String? = null,

    /** User id */
    val userId: String? = null,

    /** Whether this is the organization administrator */
    val orgAdmin: Boolean? = null,

    /** Creator id */
    val createUserId: String? = null,

    /** Creator name */
    val createUserName: String? = null,

    /** Create time */
    val createTime: LocalDateTime? = null,

    /** Updater id */
    val updateUserId: String? = null,

    /** Updater name */
    val updateUserName: String? = null,

    /** Update time */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>