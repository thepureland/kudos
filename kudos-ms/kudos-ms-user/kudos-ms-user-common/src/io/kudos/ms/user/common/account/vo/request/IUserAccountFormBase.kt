package io.kudos.ms.user.common.account.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import java.time.LocalDateTime

/**
 * User form base fields (shared between create and update)
 *
 * @author K
 * @since 1.0.0
 */
interface IUserAccountFormBase {

    /** Username */
    val username: String?

    /** Tenant ID */
    val tenantId: String?

    /** Login password */
    val loginPassword: String?

    /** Security password */
    val securityPassword: String?

    /** Account type dict code */
    val accountTypeDictCode: String?

    /** Account status dict code */
    val accountStatusDictCode: String?

    /** Default locale */
    val defaultLocale: String?

    /** Default timezone */
    val defaultTimezone: String?

    /** Default currency */
    val defaultCurrency: String?

    /** Last login time */
    val lastLoginTime: LocalDateTime?

    /** Last login IP */
    val lastLoginIp: Long?

    /** Last logout time */
    val lastLogoutTime: LocalDateTime?

    /** Login error count */
    val loginErrorTimes: Int?

    /** Security password error count */
    val securityPasswordErrorTimes: Int?

    /** Session key */
    val sessionKey: String?

    /** Authentication key */
    val authenticationKey: String?

    /** Owning organization ID */
    val orgId: String?

    /** Direct supervisor ID */
    val supervisorId: String?

    /** Remark */
    @get:MaxLength(128)
    val remark: String?
}
