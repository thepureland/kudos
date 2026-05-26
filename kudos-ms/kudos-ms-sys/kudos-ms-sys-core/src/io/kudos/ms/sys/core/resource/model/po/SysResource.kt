package io.kudos.ms.sys.core.resource.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable

/**
 * Resource database entity
 *
 * @author K
 * @since 1.0.0
 */
interface SysResource : IManagedDbEntity<String, SysResource> {

    companion object : DbEntityFactory<SysResource>()

    /** Name */
    @get:Sortable
    var name: String

    /** URL */
    var url: String?

    /** Resource type dictionary code */
    var resourceTypeDictCode: String

    /** Parent id */
    var parentId: String?

    /** Order number among siblings under the same parent */
    var orderNum: Int?

    /** Icon */
    var icon: String?

    /** Subsystem code */
    var subSystemCode: String

}