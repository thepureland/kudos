package io.kudos.ms.sys.common.param.vo.request

import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.RegExpEnum
import jakarta.validation.constraints.NotBlank

/**
 * 参数表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface ISysParamFormBase {

    /** 参数名称 */
    @get:NotBlank
    @get:MaxLength(32)
    @get:Matches(RegExpEnum.RELAXED_VAR_NAME)
    val paramName: String

    /** 参数值 */
    @get:NotBlank
    @get:MaxLength(256)
    val paramValue: String

    /** 默认参数值 */
    @get:MaxLength(256)
    val defaultValue: String?

    /** 原子服务编码 */
    @get:NotBlank
    @get:Matches(RegExpEnum.VAR_NAME)
    val atomicServiceCode: String

    /** 序号 */
    val orderNum: Int?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}
