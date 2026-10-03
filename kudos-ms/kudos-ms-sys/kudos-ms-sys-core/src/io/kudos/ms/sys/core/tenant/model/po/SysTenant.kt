package io.kudos.ms.sys.core.tenant.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable

/**
 * Tenant database entity
 *
 * @author K
 * @since 1.0.0
 */
interface SysTenant : IManagedDbEntity<String, SysTenant> {

    companion object : DbEntityFactory<SysTenant>()

    /** Name */
    @get:Sortable
    var name: String

    /** Timezone */
    var timezone: String?

    /** Default language code */
    var defaultLanguageCode: String?

    /**
     * Owning customer organization (a `user_org` root). Null for platform tenants and for every tenant
     * while organization mode is off. Changed only through the platform association flow.
     */
    var organizationId: String?

    /** Whether organization members may enter. A newly associated tenant starts closed (G-2). */
    var organizationOpen: Boolean

}