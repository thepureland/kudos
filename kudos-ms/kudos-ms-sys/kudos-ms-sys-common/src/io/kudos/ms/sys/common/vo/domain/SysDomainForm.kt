package io.kudos.ms.sys.common.vo.domain

import io.kudos.base.support.payload.FormPayload
import jakarta.validation.constraints.NotBlank


/**
 * 域名表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysDomainForm (

    /** 主键 */
    override val id: String? = null,


    /** 域名 */
    @get:NotBlank
    val domain: String = "",

    /** 系统编码 */
    @get:NotBlank
    val systemCode: String = "",

    /** 租户id */
    @get:NotBlank
    val tenantId: String = "",

    /** 备注 */
    val remark: String? = null,

) : FormPayload<String?>() {


    constructor() : this(null)


}