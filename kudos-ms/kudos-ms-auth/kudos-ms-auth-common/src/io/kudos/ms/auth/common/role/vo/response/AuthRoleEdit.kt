package io.kudos.ms.auth.common.role.vo.response

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * Response VO for editing a role.
 *
 * @author K
 * @since 1.0.0
 */
data class AuthRoleEdit (

    /** Primary key. */
    override val id: String = "",

    /** Role code. */
    val code: String? = null,

    /** Role name. */
    val name: String? = null,

    /** Tenant id. */
    val tenantId: String? = null,

    /** Subsystem code. */
    val subsysCode: String? = null,

    /** Parent role id; NULL = root role. */
    val parentId: String? = null,

    /** Remark. */
    val remark: String? = null,

) : IIdEntity<String>
