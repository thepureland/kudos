package io.kudos.ms.auth.common.grant.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import java.io.Serializable

/**
 * Request body for submitting a role-grant approval request.
 *
 * Tenant is derived server-side from the role, so the caller only supplies role + user + reason.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthRoleGrantSubmitRequest(

    /** Role to be granted. */
    val roleId: String,

    /** User the role would be granted to. */
    val userId: String,

    /** Optional justification shown to the approver. */
    @get:MaxLength(512)
    val reason: String? = null,

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
