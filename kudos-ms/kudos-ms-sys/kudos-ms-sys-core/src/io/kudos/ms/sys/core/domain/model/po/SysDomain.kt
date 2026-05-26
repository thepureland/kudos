package io.kudos.ms.sys.core.domain.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable

/**
 * Domain DB entity.
 *
 * @author K
 * @since 1.0.0
 */
interface SysDomain : IManagedDbEntity<String, SysDomain> {

    companion object : DbEntityFactory<SysDomain>()

    /** Domain */
    @get:Sortable
    var domain: String

    /** System code */
    var systemCode: String

    /** Tenant id; `null` means platform-level (mapped to `tenant_id IS NULL` in the DB) */
    var tenantId: String?

}
