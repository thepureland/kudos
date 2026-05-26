package io.kudos.ms.auth.common.group.vo.response

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * Response VO for user group edit.
 *
 * @author K
 * @since 1.0.0
 */
data class AuthGroupEdit (

    /** Primary key. */
    override val id: String = "",

    /** User group code. */
    val code: String? = null,

    /** User group name. */
    val name: String? = null,

    /** Tenant id. */
    val tenantId: String? = null,

    /** Subsystem code. */
    val subsysCode: String? = null,

    /** Remark. */
    val remark: String? = null,

) : IIdEntity<String>
