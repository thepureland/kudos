package io.kudos.ms.user.common.account.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * User list query result response VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountRow (

    /** Primary key */
    override val id: String = "",

    /** Username */
    val username: String? = null,

    /** Tenant id */
    val tenantId: String? = null,

    /** Account type dict code */
    val accountTypeDictCode: String? = null,

    /** Account status dict code */
    val accountStatusDictCode: String? = null,

    /** Default locale */
    val defaultLocale: String? = null,

    /** Default timezone */
    val defaultTimezone: String? = null,

    /** Default currency */
    val defaultCurrency: String? = null,

    /** Last login time */
    val lastLoginTime: LocalDateTime? = null,

    /** Last login IP */
    val lastLoginIp: Long? = null,

    /** Last logout time */
    val lastLogoutTime: LocalDateTime? = null,

    /** Login error count */
    val loginErrorTimes: Int? = null,

    /** Security password error count */
    val securityPasswordErrorTimes: Int? = null,

    /** Session key */
    val sessionKey: String? = null,

    /** Authentication key */
    val authenticationKey: String? = null,

    /** Organization id */
    val orgId: String? = null,

    /** Supervisor id */
    val supervisorId: String? = null,

    /** Remark */
    val remark: String? = null,

    /** Whether active */
    val active: Boolean? = null,

    /** Whether built-in */
    val builtIn: Boolean? = null,

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