package io.kudos.ms.sys.core.datasource.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable

/**
 * Data source database entity.
 *
 * @author K
 * @since 1.0.0
 */
interface SysDataSource : IManagedDbEntity<String, SysDataSource> {

    companion object : DbEntityFactory<SysDataSource>()

    /** Name */
    @get:Sortable
    var name: String

    /** Sub-system code */
    var subSystemCode: String

    /** Micro-service code */
    var microServiceCode: String?

    /** Tenant id */
    var tenantId: String?

    /** url */
    var url: String

    /** Username */
    var username: String

    /** Password */
    var password: String?

    /** Initial connection count. Initialization happens on explicit init() call or the first getConnection. */
    var initialSize: Int?

    /** Maximum number of active connections */
    var maxActive: Int?

    /** Maximum number of idle connections */
    var maxIdle: Int?

    /** Minimum number of idle connections to keep available */
    var minIdle: Int?

    /** Maximum borrow duration in milliseconds. The pool throws an exception if a borrowed connection is not returned in time. */
    var maxWait: Int?

    /** Connection lifetime in milliseconds. After this time (since initialization) the pool removes the connection on borrow or return. */
    var maxAge: Int?

}
