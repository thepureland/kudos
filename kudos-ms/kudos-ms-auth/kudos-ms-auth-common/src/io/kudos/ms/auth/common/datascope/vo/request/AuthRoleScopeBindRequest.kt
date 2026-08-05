package io.kudos.ms.auth.common.datascope.vo.request

import java.io.Serializable


/**
 * Replace a role's data-scope grants **on one dimension**.
 *
 * The dimension is part of the request rather than implied, because replace semantics scoped to the
 * whole role would mean editing orgs on one screen silently wipes the regions configured on another.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthRoleScopeBindRequest(

    /** The role. */
    val roleId: String,

    /** The dimension to write, e.g. `org`. */
    val dimension: String,

    /** The desired complete set for that dimension; empty clears it. */
    val values: List<String> = emptyList(),

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
