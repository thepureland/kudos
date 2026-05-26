package io.kudos.ms.sys.common.datasource.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * Data source cache entry.
 *
 * @author K
 * @since 1.0.0
 */
data class SysDataSourceCacheEntry (

    /** Primary key. */
    override val id: String,


    /** Name. */
    val name: String,

    /** Sub-system code. */
    val subSystemCode: String,

    /** Microservice code; `null` means unspecified (matches the nullable `micro_service_code` column). */
    val microServiceCode: String?,

    /** Tenant id. */
    val tenantId: String?,

    /** URL. */
    val url: String,

    /** Username. */
    val username: String,

    /** Password. */
    val password: String?,

    /** Initial connection count. Initialization occurs on explicit init() call or the first getConnection(). */
    val initialSize: Int?,

    /** Maximum number of active connections. */
    val maxActive: Int?,

    /** Maximum number of idle connections. */
    val maxIdle: Int?,

    /** Minimum number of idle connections to keep. */
    val minIdle: Int?,

    /** Maximum borrow duration (ms). If a connection borrowed from the pool is not returned in time, the pool throws an exception. */
    val maxWait: Int?,

    /** Connection lifetime (ms). After this timeout (relative to init time), the pool removes the connection on borrow or return. */
    val maxAge: Int?,

    /** Remark. */
    val remark: String?,

    /** Whether active. */
    val active: Boolean?,

    /** Whether built-in. */
    val builtIn: Boolean?,

) : IIdEntity<String>, Serializable {



    companion object {
        private const val serialVersionUID = 5789474834656195376L
    }

}
