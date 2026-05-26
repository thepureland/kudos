package io.kudos.ms.sys.core.dict.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable

/**
 * Dictionary database entity.
 *
 * @author K
 * @since 1.0.0
 */
interface SysDict : IManagedDbEntity<String, SysDict> {

    companion object : DbEntityFactory<SysDict>()

    /** Dictionary type */
    @get:Sortable
    var dictType: String

    /** Dictionary name */
    @get:Sortable
    var dictName: String

    /** Atomic service code */
    var atomicServiceCode: String

}
