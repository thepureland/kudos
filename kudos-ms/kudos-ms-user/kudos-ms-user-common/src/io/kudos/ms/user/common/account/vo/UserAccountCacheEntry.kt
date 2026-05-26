package io.kudos.ms.user.common.account.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * User cache entry
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountCacheEntry (

    /** Primary key */
    override val id: String,

    /** Username */
    val username: String?,

    /** Tenant ID */
    val tenantId: String?,

    /** Login password */
    val loginPassword: String?,

    /** Security password */
    val securityPassword: String?,

    /** Account type dict code */
    val accountTypeDictCode: String?,

    /** Account status dict code */
    val accountStatusDictCode: String?,

    /** Default locale */
    val defaultLocale: String?,

    /** Default timezone */
    val defaultTimezone: String?,

    /** Default currency */
    val defaultCurrency: String?,

    /** Last login time */
    val lastLoginTime: LocalDateTime?,

    /** Last login IP */
    val lastLoginIp: Long?,

    /** Last logout time */
    val lastLogoutTime: LocalDateTime?,

    /** Login error count */
    val loginErrorTimes: Int?,

    /** Security password error count */
    val securityPasswordErrorTimes: Int?,

    /** Session key */
    val sessionKey: String?,

    /** Authentication key */
    val authenticationKey: String?,

    /** Owning organization ID */
    val orgId: String?,

    /** Direct supervisor ID */
    val supervisorId: String?,

    /** Remark */
    val remark: String?,

    /** Freeze type dict code; non-null means a freeze record exists */
    val freezeType: String? = null,

    /** Freeze record creation time */
    val freezeTime: LocalDateTime? = null,

    /** Freeze effective start time; null means effective immediately */
    val freezeStartTime: LocalDateTime? = null,

    /** Freeze expiry time; null means permanently frozen */
    val freezeEndTime: LocalDateTime? = null,

    /** Freeze reason title */
    val freezeTitle: String? = null,

    /** Freeze detailed description */
    val freezeContent: String? = null,

    /** Whether active */
    val active: Boolean?,

    /** Whether built-in */
    val builtIn: Boolean?,

    /** Creator id */
    val createUserId: String?,

    /** Creator name */
    val createUserName: String?,

    /** Create time */
    val createTime: LocalDateTime?,

    /** Updater id */
    val updateUserId: String?,

    /** Updater name */
    val updateUserName: String?,

    /** Update time */
    val updateTime: LocalDateTime?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

}
