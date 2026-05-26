package io.kudos.ms.sys.core.cache.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable

/**
 * Cache configuration DB entity.
 *
 * @author K
 * @since 1.0.0
 */
interface SysCache : IManagedDbEntity<String, SysCache> {

    companion object : DbEntityFactory<SysCache>()

    /** Name */
    @get:Sortable
    var name: String

    /** Atomic service code */
    var atomicServiceCode: String

    /** Cache strategy code */
    var strategyDictCode: String

    /** Write to cache on boot */
    var writeOnBoot: Boolean

    /** Write back to cache in real time */
    var writeInTime: Boolean

    /** Cache TTL (seconds) */
    var ttl: Int?

    /** Whether it is a Hash cache (when true, participates in MixHashCacheManager initialization) */
    var hash: Boolean

}