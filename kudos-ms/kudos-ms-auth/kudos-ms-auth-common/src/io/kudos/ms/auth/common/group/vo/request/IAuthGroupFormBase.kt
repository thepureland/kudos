package io.kudos.ms.auth.common.group.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength

/**
 * Base fields for the user group form (shared by create and update).
 *
 * @author K
 * @since 1.0.0
 */
interface IAuthGroupFormBase {

    /** User group code. */
    val code: String?

    /** User group name. */
    val name: String?

    /** Tenant id. */
    val tenantId: String?

    /** Subsystem code. */
    val subsysCode: String?

    /** Remark. */
    @get:MaxLength(128)
    val remark: String?
}
