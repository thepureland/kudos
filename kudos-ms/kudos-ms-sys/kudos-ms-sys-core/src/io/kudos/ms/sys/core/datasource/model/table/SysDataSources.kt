package io.kudos.ms.sys.core.datasource.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.datasource.model.po.SysDataSource
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * Data source table-to-entity binding.
 *
 * @author K
 * @since 1.0.0
 */
object SysDataSources : ManagedTable<SysDataSource>("sys_data_source") {

    /** Name */
    var name = varchar("name").bindTo { it.name }

    /** Sub-system code */
    var subSystemCode = varchar("sub_system_code").bindTo { it.subSystemCode }

    /** Micro-service code */
    var microServiceCode = varchar("micro_service_code").bindTo { it.microServiceCode }

    /** Tenant id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** url */
    var url = varchar("url").bindTo { it.url }

    /** Username */
    var username = varchar("username").bindTo { it.username }

    /** Password */
    var password = varchar("password").bindTo { it.password }

    /** Initial connection count. Initialization happens on an explicit init() call or the first getConnection. */
    var initialSize = int("initial_size").bindTo { it.initialSize }

    /** Max active connections */
    var maxActive = int("max_active").bindTo { it.maxActive }

    /** Max idle connections */
    var maxIdle = int("max_idle").bindTo { it.maxIdle }

    /** Minimum idle connections; the pool maintains at least this many idle connections. */
    var minIdle = int("min_idle").bindTo { it.minIdle }

    /** Maximum loan duration (ms). If a client borrows a connection and does not return it before the timeout, the pool throws an exception. */
    var maxWait = int("max_wait").bindTo { it.maxWait }

    /** Connection lifetime (ms). After this duration (since initialization), the pool removes the connection on next borrow or return. */
    var maxAge = int("max_age").bindTo { it.maxAge }




}
