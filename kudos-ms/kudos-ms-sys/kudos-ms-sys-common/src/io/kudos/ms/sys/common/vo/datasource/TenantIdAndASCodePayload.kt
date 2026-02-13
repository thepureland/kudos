package io.kudos.ms.sys.common.vo.datasource

import jakarta.validation.constraints.NotBlank

/**
 * 租户ID和原子服务编码的Payload
 *
 * @author K
 * @since 1.0.0
 */
data class TenantIdAndASCodePayload(

    /** 租户ID */
    @get:NotBlank(message = "租户ID不能为空！")
    var tenantId: String ,

    /** 原子服务编码 */
    @get:NotBlank(message = "原子服务编码不能为空！")
    var atomicServiceCode : String = "default"

) {

    constructor() : this("")

}