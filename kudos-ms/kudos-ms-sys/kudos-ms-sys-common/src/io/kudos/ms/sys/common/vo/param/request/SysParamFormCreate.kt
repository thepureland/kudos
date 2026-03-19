package io.kudos.ms.sys.common.vo.param.request

import jakarta.validation.constraints.NotBlank


/**
 * 参数表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamFormCreate (

    /** 参数名称 */
    @get:NotBlank
    val paramName: String = "",

    /** 参数值 */
    @get:NotBlank
    val paramValue: String = "",

    /** 默认参数值 */
    val defaultValue: String? = null,

    /** 原子服务编码 */
    @get:NotBlank
    val atomicServiceCode: String = "",

    /** 序号 */
    val orderNum: Int? = null,

    /** 备注 */
    val remark: String? = null,

)