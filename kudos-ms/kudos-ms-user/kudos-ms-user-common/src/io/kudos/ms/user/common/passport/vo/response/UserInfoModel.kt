package io.kudos.ms.user.common.passport.vo.response

import java.io.Serializable
import java.time.LocalDateTime


/**
 * Snapshot of user info returned to the caller after a successful login.
 *
 * Difference from [io.kudos.ms.user.common.account.vo.UserAccountCacheEntry]:
 * the latter is an internal type for the cache layer (containing all fields); this class only exposes fields safe to publish after login.
 *
 * @author K
 * @since 1.0.0
 */
data class UserInfoModel(

    /** User primary key */
    val id: String,

    /** Username */
    val username: String,

    /** Tenant id */
    val tenantId: String,

    /** Organization id the user belongs to; may be null */
    val orgId: String?,

    /** Account type dictionary code */
    val accountTypeDictCode: String?,

    /** Default locale */
    val defaultLocale: String?,

    /** Default timezone */
    val defaultTimezone: String?,

    /** Default currency */
    val defaultCurrency: String?,

    /** Login time of this session (server time) */
    val loginTime: LocalDateTime,

) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

}
