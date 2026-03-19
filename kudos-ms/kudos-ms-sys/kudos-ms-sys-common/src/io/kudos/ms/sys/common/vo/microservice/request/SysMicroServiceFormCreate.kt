package io.kudos.ms.sys.common.vo.microservice.request

import jakarta.validation.constraints.NotBlank


/**
 * 微服务表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceFormCreate (

    /** 编码 */
    @get:NotBlank
    val code: String = "",

    /** 名称 */
    @get:NotBlank
    val name: String = "",

    /** 上下文 */
    @get:NotBlank
    val context: String = "",

    /** 是否为原子服务 */
    val atomicService: Boolean = true,

    /** 父服务编码 */
    val parentCode: String? = null,

    /** 备注 */
    val remark: String? = null,

)