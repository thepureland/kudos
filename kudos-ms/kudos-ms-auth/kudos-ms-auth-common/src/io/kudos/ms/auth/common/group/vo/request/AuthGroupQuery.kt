package io.kudos.ms.auth.common.group.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.auth.common.group.vo.response.AuthGroupRow


/**
 * Request VO with query conditions for the user group list.
 *
 * @author K
 * @since 1.0.0
 */
data class AuthGroupQuery (

    /** User group code. */
    val code: String? = null,

    /** User group name. */
    val name: String? = null,

    /** Tenant id. */
    val tenantId: String? = null,

    /** Subsystem code. */
    val subsysCode: String? = null,

    /** Whether the group is active. */
    val active: Boolean? = null,

    /** Whether the group is built-in. */
    val builtIn: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = AuthGroupRow::class

}