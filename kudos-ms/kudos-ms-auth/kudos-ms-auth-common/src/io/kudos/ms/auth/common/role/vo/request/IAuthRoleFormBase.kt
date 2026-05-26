package io.kudos.ms.auth.common.role.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength

/**
 * Base fields for the role form (shared by create and update).
 *
 * @author K
 * @since 1.0.0
 */
interface IAuthRoleFormBase {

    /** Role code. */
    val code: String?

    /** Role name. */
    val name: String?

    /** Tenant id. */
    val tenantId: String?

    /** Subsystem code. */
    val subsysCode: String?

    /** Remark. */
    @get:MaxLength(128)
    val remark: String?
}
