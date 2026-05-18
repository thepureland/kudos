package io.kudos.ms.sys.common.locale.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import jakarta.validation.constraints.NotBlank

/**
 * 语言/区域字典表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface ISysLocaleFormBase {

    /** 语言代码 */
    @get:NotBlank
    @get:MaxLength(32)
    val code: String

    /** 显示名称 */
    @get:NotBlank
    @get:MaxLength(64)
    val displayName: String

    /** 英文名称 */
    @get:NotBlank
    @get:MaxLength(64)
    val englishName: String

    /** 排序号 */
    val sortNo: Int

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}
