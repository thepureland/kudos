package io.kudos.ms.auth.common.group.vo.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * Request VO for user group form update.
 *
 * @author K
 * @since 1.0.0
 */
data class AuthGroupFormUpdate (

    /** Primary key. */
    override val id: String,

    override val code: String?,

    override val name: String?,

    override val tenantId: String?,

    override val subsysCode: String?,

    override val remark: String?,

) : IIdEntity<String>, IAuthGroupFormBase
