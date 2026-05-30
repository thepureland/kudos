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

    /** Whether assigning this role requires an approval workflow. Null defaults to false. */
    val approvalRequired: Boolean?

    /**
     * Data-scope policy code (see DataScopeEnum: ALL / ORG_AND_CHILD / ORG / SELF / CUSTOM).
     * Null is treated as ALL (no row restriction). CUSTOM additionally uses the role's
     * auth_role_org grants, managed separately via the data-scope admin endpoint.
     */
    val dataScope: String?

    /** Remark. */
    @get:MaxLength(128)
    val remark: String?
}
