package io.kudos.ms.sys.common.datasource.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Data source detail response VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysDataSourceDetail (

    /** Primary key. */
    override val id: String = "",


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

    /** Creator id. */
    val createUserId: String? = null,

    /** Creator name. */
    val createUserName: String? = null,

    /** Create time. */
    val createTime: LocalDateTime? = null,

    /** Updater id. */
    val updateUserId: String? = null,

    /** Updater name. */
    val updateUserName: String? = null,

    /** Update time. */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>