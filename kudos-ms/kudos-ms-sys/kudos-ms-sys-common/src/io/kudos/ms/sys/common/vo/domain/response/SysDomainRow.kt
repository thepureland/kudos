package io.kudos.ms.sys.common.vo.domain.response

import java.time.LocalDateTime


/**
 * 域名列表查询结果响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysDomainRow (

    /** 主键 */
    val id: String = "",

    /** 域名 */
    val domain: String = "",

    /** 系统编码 */
    val systemCode: String = "",

    /** 租户id */
    val tenantId: String = "",

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = false,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

) {


    /** 租户名称 */
    var tenantName: String = ""


}