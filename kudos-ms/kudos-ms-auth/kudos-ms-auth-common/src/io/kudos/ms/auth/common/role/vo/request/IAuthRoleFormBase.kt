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

    /**
     * Parent role id (NULL = root role). Service-side validation:
     *  - must reference an existing role in the same tenant + subsystem
     *  - must not be the role itself (self-cycle)
     *  - must not be a descendant of this role (introduces a cycle)
     */
    val parentId: String?

    /** Remark. */
    @get:MaxLength(128)
    val remark: String?
}
