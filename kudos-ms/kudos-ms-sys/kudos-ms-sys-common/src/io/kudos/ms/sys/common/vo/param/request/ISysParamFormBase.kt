package io.kudos.ms.sys.common.vo.param.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
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
    val paramName: String

    /** 参数值 */
    @get:NotBlank
    val paramValue: String

    /** 默认参数值 */
    val defaultValue: String?

    /** 原子服务编码 */
    @get:NotBlank
    val atomicServiceCode: String

    /** 序号 */
    val orderNum: Int?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}
