package io.kudos.ms.auth.common.grant.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime

/**
 * Detail VO for a role-grant approval request.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthRoleGrantRequestDetail(

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
    val createUserId: String? = null,
    val createUserName: String? = null,
    val createTime: LocalDateTime? = null,
    val updateUserId: String? = null,
    val updateUserName: String? = null,
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>
