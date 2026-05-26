package io.kudos.ms.sys.common.outline.vo.response

import java.time.LocalDateTime


/**
 * Response VO for outbound whitelist list query result.
 *
 * @author K
 * @since 1.0.0
 */
data class SysOutLineRow(

    /** Primary key */
    val id: String = "",

    /** Name */
    val name: String = "",

    /** Hostname or wildcard */
    val host: String = "",

    /** Port */
    val port: Int? = null,

    /** Protocol */
    val protocol: String = "",

    /** System code */
    val systemCode: String = "",

    /** Tenant id */
    val tenantId: String? = null,

    /** Remark */
    val remark: String? = null,

    /** Whether enabled */
    val active: Boolean = true,

    /** Whether built-in */
    val builtIn: Boolean = false,

    /** Creation time */
    val createTime: LocalDateTime? = null,
)
