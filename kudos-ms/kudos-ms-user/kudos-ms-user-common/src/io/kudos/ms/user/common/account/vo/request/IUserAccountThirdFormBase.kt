package io.kudos.ms.user.common.account.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import java.time.LocalDateTime

/**
 * User third-party account form base fields (shared between create and update)
 *
 * @author K
 * @since 1.0.0
 */
interface IUserAccountThirdFormBase {

    /** Associated user account ID */
    val userId: String?

    /** Third-party platform dictionary code */
    val accountProviderDictCode: String?

    /** Issuer / platform tenant */
    val accountProviderIssuer: String?

    /** Third-party user unique identifier */
    val subject: String?

    /** Cross-application unified identifier */
    val unionId: String?

    /** Third-party display name */
    val externalDisplayName: String?

    /** Third-party email */
    val externalEmail: String?

    /** Avatar URL */
    val avatarUrl: String?

    /** Last login time */
    val lastLoginTime: LocalDateTime?

    /** Tenant ID */
    val tenantId: String?

    /** Remark */
    @get:MaxLength(128)
    val remark: String?
}
