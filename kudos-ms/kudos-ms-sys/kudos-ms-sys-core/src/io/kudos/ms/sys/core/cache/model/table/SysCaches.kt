package io.kudos.ms.sys.core.cache.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.cache.model.po.SysCache
import org.ktorm.schema.boolean
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * Cache configuration table-to-entity binding.
 *
 * @author K
 * @since 1.0.0
 */
object SysCaches : ManagedTable<SysCache>("sys_cache") {

    /** Name */
    var name = varchar("name").bindTo { it.name }

    /** Atomic service code */
    var atomicServiceCode = varchar("atomic_service_code").bindTo { it.atomicServiceCode }

    /** Cache strategy code */
    var strategyDictCode = varchar("strategy_dict_code").bindTo { it.strategyDictCode }

    /** Write to cache on boot */
    var writeOnBoot = boolean("write_on_boot").bindTo { it.writeOnBoot }

    /** Write back to cache in real time */
    var writeInTime = boolean("write_in_time").bindTo { it.writeInTime }

    /** Cache TTL (seconds) */
    var ttl = int("ttl").bindTo { it.ttl }

    /** Whether it is a Hash cache */
    var hash = boolean("hash").bindTo { it.hash }



}