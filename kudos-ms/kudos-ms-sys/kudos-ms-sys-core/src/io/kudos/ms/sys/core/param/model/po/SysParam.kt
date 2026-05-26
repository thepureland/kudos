package io.kudos.ms.sys.core.param.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable

/**
 * Parameter database entity
 *
 * @author K
 * @since 1.0.0
 */
interface SysParam : IManagedDbEntity<String, SysParam> {

    companion object : DbEntityFactory<SysParam>()

    /** Parameter name */
    @get:Sortable
    var paramName: String

    /** Parameter value */
    var paramValue: String

    /** Default parameter value */
    var defaultValue: String?

    /** Atomic service code */
    var atomicServiceCode: String

    /** Order number */
    var orderNum: Int?

}