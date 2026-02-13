package io.kudos.ms.sys.common.vo.dict

import jakarta.validation.constraints.NotBlank

/**
 * 字典类型和原子服务编码的Payload
 *
 * @author K
 * @since 1.0.0
 */
data class DictTypeAndASCodePayload(

    /** 字典类型 */
    @get:NotBlank(message = "字典类型不能为空！")
    var dictType: String,

    /** 原子服务编码 */
    @get:NotBlank(message = "原子服务编码不能为空！")
    var atomicServiceCode : String = "default"

) {

    constructor() : this("")

}