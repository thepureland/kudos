package io.kudos.ms.auth.common.datascope.vo.request

import java.io.Serializable

/**
 * Request body to set a role's custom data-scope orgs (used when the role's data_scope = CUSTOM).
 *
 * Replace semantics: the supplied [orgIds] become the complete custom org set for [roleId];
 * anything previously bound and not present here is removed. An empty list clears all grants.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthRoleOrgBindRequest(

    /** The role whose custom org grants are being set. */
    val roleId: String,

    /** The complete desired set of org ids for the role (replace, not merge). */
    val orgIds: List<String> = emptyList(),

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
