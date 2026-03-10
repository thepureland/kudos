package io.kudos.ms.sys.common.vo.microservice

import io.kudos.base.support.payload.FormPayload
import jakarta.validation.constraints.NotBlank


/**
 * 微服务表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceForm (

    //region your codes 1

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

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    override val id: String
        get() = code

    // endregion your codes 3

}
