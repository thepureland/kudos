package io.kudos.ms.auth.common.grant.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import java.io.Serializable

/**
 * Request body for approving or rejecting a grant request.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthRoleGrantDecisionRequest(

    /** Id of the pending request being decided. */
    val id: String,

    /** Optional approver comment. */
    @get:MaxLength(512)
    val comment: String? = null,

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
