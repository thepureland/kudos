package io.kudos.ms.sys.common.outline.vo.response

import java.time.LocalDateTime


/**
 * 出网白名单列表查询结果响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysOutLineRow(

    /** 主键 */
    val id: String = "",

    /** 名称 */
    val name: String = "",

    /** 主机名或通配符 */
    val host: String = "",

    /** 端口 */
    val port: Int? = null,

    /** 协议 */
    val protocol: String = "",

    /** 系统编码 */
    val systemCode: String = "",

    /** 租户id */
    val tenantId: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = false,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,
)
