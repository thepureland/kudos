package io.kudos.ms.sys.common.vo.param

import jakarta.validation.constraints.NotBlank

/**
 * 参数名称和原子服务编码的Payload
 *
 * @author K
 * @since 1.0.0
 */
data class ParamNameAndASCodePayload(

    /** 参数名称 */
    @get:NotBlank(message = "参数名称不能为空！")
    var paramName: String,

    /** 原子服务编码 */
    @get:NotBlank(message = "原子服务编码不能为空！")
    var atomicServiceCode : String = "default"

) {

    constructor() : this("")

}