package io.kudos.ms.sys.common.accessrule.vo.request
import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime


/**
 * IP访问规则表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpFormCreate (

    override val ipStart: Long? ,

    override val ipEnd: Long? ,

    override val ipTypeDictCode: String ,

    override val expirationDate: LocalDateTime? ,

    /** 系统编码 */
    @get:NotBlank
    @get:MaxLength(32)
    val systemCode: String,

    /** 租户id */
    @get:NotBlank
    @get:FixedLength(36)
    val tenantId: String,

    override val remark: String? ,

    override val active: Boolean? ,

) : ISysAccessRuleIpFormBase
