package io.kudos.ms.sys.common.vo.domain.request

import jakarta.validation.constraints.NotBlank


/**
 * 域名表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysDomainFormCreate (

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

)