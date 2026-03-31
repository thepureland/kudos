package io.kudos.ms.sys.common.vo.dict.request

import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.RegExpEnum
import jakarta.validation.constraints.NotBlank

/**
 * 字典表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface ISysDictFormBase {

    /** 字典类型 */
    @get:NotBlank
    @get:MaxLength(64)
    @get:Matches(RegExpEnum.VAR_NAME)
    val dictType: String

    /** 字典名称 */
    @get:NotBlank
    @get:MaxLength(64)
    val dictName: String

    /** 原子服务编码 */
    @get:NotBlank
    @get:Matches(RegExpEnum.VAR_NAME)
    @get:MaxLength(32)
    val atomicServiceCode: String

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}
