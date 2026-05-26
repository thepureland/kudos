package io.kudos.ms.user.common.account.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * User third-party account cache entry
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountThirdCacheEntry (

    /** Primary key */
    override val id: String,

    /** Linked user account ID */
    val userId: String?,

    /** Third-party platform dict code */
    val accountProviderDictCode: String?,

    /** Issuer / platform tenant */
    val accountProviderIssuer: String?,

    /** Third-party user unique identifier */
    val subject: String?,

    /** Cross-application unified identifier */
    val unionId: String?,

    /** Third-party display name */
    val externalDisplayName: String?,

    /** Third-party email */
    val externalEmail: String?,

    /** Avatar URL */
    val avatarUrl: String?,

    /** Last login time */
    val lastLoginTime: LocalDateTime?,

    /** Tenant ID */
    val tenantId: String?,

    /** Remark */
    val remark: String?,

    /** Whether active */
    val active: Boolean?,

    /** Whether built-in */
    val builtIn: Boolean?,

    /** Creator user ID */
    val createUserId: String?,

    /** Creator user name */
    val createUserName: String?,

    /** Create time */
    val createTime: LocalDateTime?,

    /** Updater user ID */
    val updateUserId: String?,

    /** Updater user name */
    val updateUserName: String?,

    /** Update time */
    val updateTime: LocalDateTime?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

}
