package io.kudos.ms.auth.common.exclusion.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime

/**
 * List row VO for an SoD exclusion pair.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthRoleExclusionRow(

    override val id: String = "",
    val roleAId: String? = null,
    val roleBId: String? = null,
    val tenantId: String? = null,
    val description: String? = null,
    val createTime: LocalDateTime? = null,
    val createUserName: String? = null,

) : IIdEntity<String>
