package io.kudos.ms.user.common.account.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.user.common.account.vo.response.UserAccountThirdRow


/**
 * User third-party account list query request VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountThirdQuery (

    /** Associated user account ID */
    val userId: String? = null,

    /** Third-party platform dictionary code */
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

    /** Tenant ID */
    val tenantId: String? = null,

    /** Whether active */
    val active: Boolean? = null,

    /** Whether built-in */
    val builtIn: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = UserAccountThirdRow::class

}