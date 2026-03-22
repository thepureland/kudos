package io.kudos.ms.sys.common.vo.dict.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import jakarta.validation.constraints.NotBlank


/**
 * 字典表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictFormCreate (

    /** 字典类型 */
    @get:NotBlank
    val dictType: String = "",

    /** 字典名称 */
    @get:NotBlank
    val dictName: String = "",

    /** 原子服务编码 */
    @get:NotBlank
    val atomicServiceCode: String = "",

    /** 备注 */
    @get:MaxLength(128)
    val remark: String? = null,

)