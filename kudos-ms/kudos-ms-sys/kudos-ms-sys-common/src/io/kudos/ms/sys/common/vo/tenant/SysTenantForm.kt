package io.kudos.ms.sys.common.vo.tenant

import io.kudos.base.model.payload.FormPayload
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty


/**
 * 租户表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantForm (

    /** 主键 */
    override val id: String? = null,


    /** 名称 */
    @get:NotBlank
    val name: String = "",

    /** 所属子系统 */
    @get:NotEmpty
    var subSystemCodes: Set<String> = emptySet(),

    /** 时区 */
    val timezone: String? = null,

    /** 默认语言编码 */
    val defaultLanguageCode: String? = null,

    /** 备注 */
    val remark: String? = null,

) : FormPayload<String?>() {


    constructor() : this(null)


}