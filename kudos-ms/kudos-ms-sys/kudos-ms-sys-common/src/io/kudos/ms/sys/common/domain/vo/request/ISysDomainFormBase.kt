package io.kudos.ms.sys.common.domain.vo.request
import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.RegExpEnum
import jakarta.validation.constraints.NotBlank

/**
 * 域名表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface ISysDomainFormBase {

    /** 域名 */
    @get:NotBlank
    @get:MaxLength(256)
    @get:Matches(RegExpEnum.DOMAIN)
    val domain: String

    /** 系统编码 */
    @get:NotBlank
    @get:MaxLength(32)
    val systemCode: String

    /** 租户id */
    @get:NotBlank
    val tenantId: String

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}
