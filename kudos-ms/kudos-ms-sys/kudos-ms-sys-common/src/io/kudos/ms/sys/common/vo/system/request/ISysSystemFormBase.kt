package io.kudos.ms.sys.common.vo.system.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import jakarta.validation.constraints.NotBlank

/**
 * 系统表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface ISysSystemFormBase {

    /** 编码 */
    @get:NotBlank
    val code: String

    /** 名称 */
    @get:NotBlank
    val name: String

    /** 是否子系统 */
    val subSystem: Boolean

    /** 父系统编号 */
    val parentCode: String?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}
