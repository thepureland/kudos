package io.kudos.ms.sys.common.datasource.vo.response

/**
 * Data source list query result response VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysDataSourceRow (

    /** Primary key. */
    val id: String = "",

    /** Name. */
    val name: String = "",

    /** Sub-system code. */
    val subSystemCode: String = "",

    /** Microservice code. */
    val microServiceCode: String = "",

    /** Tenant id. */
    val tenantId: String? = null,

    /** Tenant name. */
    var tenantName: String? = null,

    /** URL. */
    val url: String = "",

    /** Username. */
    val username: String = "",

    /** Password. */
    val password: String? = null,

    /** Initial connection count. Initialization occurs on explicit init() call or the first getConnection(). */
    val initialSize: Int? = null,

    /** Maximum number of active connections. */
    val maxActive: Int? = null,

    /** Maximum number of idle connections. */
    val maxIdle: Int? = null,

    /** Minimum number of idle connections to keep. */
    val minIdle: Int? = null,

    /** Maximum borrow duration (ms). If a connection borrowed from the pool is not returned in time, the pool throws an exception. */
    val maxWait: Int? = null,

    /** Connection lifetime (ms). After this timeout (relative to init time), the pool removes the connection on borrow or return. */
    val maxAge: Int? = null,

    /** Remark. */
    val remark: String? = null,

    /** Whether active. */
    val active: Boolean? = null,

    /** Whether built-in. */
    val builtIn: Boolean? = null,

)