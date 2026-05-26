package io.kudos.ms.sys.common.accessrule.vo.request

import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import jakarta.validation.constraints.NotBlank

/**
 * Access rule create form request VO.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class SysAccessRuleFormCreate (

    /** Tenant id. */
    @get:NotBlank
    @get:FixedLength(36)
    val tenantId: String,

    /** System code. */
    @get:NotBlank
    @get:MaxLength(32)
    val systemCode: String,

    override val accessRuleTypeDictCode: String,

    override val remark: String?,

) : ISysAccessRuleFormBase
