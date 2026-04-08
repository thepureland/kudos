package io.kudos.ms.sys.common.accessrule.vo.request

import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import jakarta.validation.constraints.NotBlank

/**
 * 访问规则表单新建请求 VO。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class SysAccessRuleFormCreate (

    /** 租户id */
    @get:NotBlank
    @get:FixedLength(36)
    val tenantId: String,

    /** 系统编码 */
    @get:NotBlank
    @get:MaxLength(32)
    val systemCode: String,

    override val accessRuleTypeDictCode: String,

    override val remark: String?,

) : ISysAccessRuleFormBase
