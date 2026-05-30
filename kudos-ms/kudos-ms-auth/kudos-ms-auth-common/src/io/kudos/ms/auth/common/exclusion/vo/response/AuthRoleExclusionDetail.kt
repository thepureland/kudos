package io.kudos.ms.auth.common.exclusion.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime

/**
 * Detail VO for an SoD exclusion pair.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthRoleExclusionDetail(

    override val id: String = "",
    val roleAId: String? = null,
    val roleBId: String? = null,
    val tenantId: String? = null,
    val description: String? = null,
    val createUserId: String? = null,
    val createUserName: String? = null,
    val createTime: LocalDateTime? = null,
    val updateUserId: String? = null,
    val updateUserName: String? = null,
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>
