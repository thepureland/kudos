package io.kudos.ms.sys.common.accessrule.vo.request
import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime


/**
 * IP访问规则表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpFormUpdate (

    @get:NotBlank
    @get:FixedLength(36)
    override val id: String,

    override val ipStart: Long?,

    override val ipEnd: Long?,

    override val ipTypeDictCode: String,

    override val expirationDate: LocalDateTime?,

    /** 父规则id */
    @get:NotBlank
    @get:FixedLength(36)
    val parentRuleId: String,

    override val remark: String?,

    override val active: Boolean?,

) : IIdEntity<String>, ISysAccessRuleIpFormBase
