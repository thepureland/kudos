package io.kudos.ms.auth.common.grant.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime

/**
 * List row VO for a role-grant approval request.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthRoleGrantRequestRow(

    override val id: String = "",
    val roleId: String? = null,
    val userId: String? = null,
    val tenantId: String? = null,
    val status: String? = null,
    val reason: String? = null,
    val requesterId: String? = null,
    val requestTime: LocalDateTime? = null,
    val approverId: String? = null,
    val decisionComment: String? = null,
    val decisionTime: LocalDateTime? = null,

) : IIdEntity<String>
