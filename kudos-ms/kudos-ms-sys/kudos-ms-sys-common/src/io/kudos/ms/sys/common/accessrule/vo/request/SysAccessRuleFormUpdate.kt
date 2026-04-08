package io.kudos.ms.sys.common.accessrule.vo.request
import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size


/**
 * 访问规则表单更新请求 VO。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class SysAccessRuleFormUpdate (

    /** 主键 */
    @get:NotBlank
    @get:FixedLength(36)
    override val id: String,

    override val accessRuleTypeDictCode: String,

    override val remark: String?,

) : IIdEntity<String>, ISysAccessRuleFormBase
