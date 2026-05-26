package io.kudos.ms.user.common.account.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * User edit response VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountEdit (

    /** Primary key */
    override val id: String = "",

    /** Username */
    val username: String? = null,

    /** Tenant ID */
    val tenantId: String? = null,

    /** Login password */
    val loginPassword: String? = null,

    /** Security password */
    val securityPassword: String? = null,

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

    /** Owning organization ID */
    val orgId: String? = null,

    /** Direct supervisor ID */
    val supervisorId: String? = null,

    /** Remark */
    val remark: String? = null,

) : IIdEntity<String>
