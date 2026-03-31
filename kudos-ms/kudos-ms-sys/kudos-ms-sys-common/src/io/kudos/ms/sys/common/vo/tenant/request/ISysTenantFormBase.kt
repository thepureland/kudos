package io.kudos.ms.sys.common.vo.tenant.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

/**
 * 租户表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface ISysTenantFormBase {

    /** 名称 */
    @get:NotBlank
    val name: String

    /** 所属子系统 */
    @get:NotEmpty
    var subSystemCodes: Set<String>

    /** 时区 */
    val timezone: String?

    /** 默认语言编码 */
    val defaultLanguageCode: String?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}
