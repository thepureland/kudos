package io.kudos.ms.sys.common.system.vo.request

import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.RegExpEnum
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
    @get:MaxLength(32)
    @get:Matches(RegExpEnum.RELAXED_VAR_NAME)
    val code: String

    /** 名称 */
    @get:NotBlank
    @get:MaxLength(128)
    val name: String

    /** 是否子系统 */
    val subSystem: Boolean

    /** 父系统编号 */
    @get:MaxLength(32)
    val parentCode: String?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}
