package io.kudos.ms.auth.common.role.vo.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * Request VO for role form update.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthRoleFormUpdate (

    /** Primary key. */
    override val id: String,

    override val code: String?,

    override val name: String?,

    override val tenantId: String?,

    override val subsysCode: String?,

    override val parentId: String? = null,

    override val approvalRequired: Boolean? = false,

    override val delegableMax: Int? = 0,

    override val dataScope: String? = null,

    override val remark: String?,

) : IIdEntity<String>, IAuthRoleFormBase
