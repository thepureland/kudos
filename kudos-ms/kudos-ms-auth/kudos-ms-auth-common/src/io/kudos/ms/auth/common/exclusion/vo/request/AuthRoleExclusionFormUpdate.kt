package io.kudos.ms.auth.common.exclusion.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.model.contract.entity.IIdEntity

/**
 * Request VO for updating an SoD exclusion pair.
 * Only [description] is mutable — to change the pair itself, delete and re-create.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthRoleExclusionFormUpdate(

    override val id: String,

    @get:MaxLength(256)
    val description: String? = null,

) : IIdEntity<String>
