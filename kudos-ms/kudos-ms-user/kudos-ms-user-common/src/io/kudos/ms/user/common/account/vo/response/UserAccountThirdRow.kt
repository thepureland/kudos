package io.kudos.ms.user.common.account.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * User third-party account list query result response VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountThirdRow (

    /** Primary key */
    override val id: String = "",

    /** Linked user account ID */
    val userId: String? = null,

    /** Third-party platform dict code */
    val accountProviderDictCode: String? = null,

    /** Issuer / platform tenant */
    val accountProviderIssuer: String? = null,

    /** Third-party user unique identifier */
    val subject: String? = null,

    /** Cross-application unified identifier */
    val unionId: String? = null,

    /** Third-party display name */
    val externalDisplayName: String? = null,

    /** Third-party email */
    val externalEmail: String? = null,

    /** Avatar URL */
    val avatarUrl: String? = null,

    /** Last login time */
    val lastLoginTime: LocalDateTime? = null,

    /** Tenant ID */
    val tenantId: String? = null,

    /** Remark */
    val remark: String? = null,

    /** Whether active */
    val active: Boolean? = null,

    /** Whether built-in */
    val builtIn: Boolean? = null,

    /** Creator user ID */
    val createUserId: String? = null,

    /** Creator user name */
    val createUserName: String? = null,

    /** Create time */
    val createTime: LocalDateTime? = null,

    /** Updater user ID */
    val updateUserId: String? = null,

    /** Updater user name */
    val updateUserName: String? = null,

    /** Update time */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>