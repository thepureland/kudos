package io.kudos.ms.sys.core.tenant.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity

/**
 * Tenant-resource relationship database entity
 *
 * @author K
 * @since 1.0.0
 */
interface SysTenantResource : IDbEntity<String, SysTenantResource> {

    companion object : DbEntityFactory<SysTenantResource>()

    /** Tenant id */
    var tenantId: String

    /** Resource id */
    var resourceId: String

}