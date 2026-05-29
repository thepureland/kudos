package io.kudos.ms.auth.common.role.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.auth.common.role.vo.response.AuthRoleRow


/**
 * Request VO for the role list query criteria.
 *
 * @author K
 * @since 1.0.0
 */
data class AuthRoleQuery (

    /** Role code. */
    val code: String? = null,

    /** Role name. */
    val name: String? = null,

    /** Tenant id. */
    val tenantId: String? = null,

    /** Subsystem code. */
    val subsysCode: String? = null,

    /** Filter roles by direct parent (NULL filter = no constraint; querying for roots needs a special UI affordance — leaving Boolean here would force a flag-vs-value collision). */
    val parentId: String? = null,

    /** Remark. */
    val remark: String? = null,

    /** Whether the role is active. */
    val active: Boolean? = null,

    /** Whether the role is built-in. */
    val builtIn: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = AuthRoleRow::class

}