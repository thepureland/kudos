package io.kudos.ms.sys.core.dict.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable

/**
 * Dictionary item database entity.
 *
 * @author K
 * @since 1.0.0
 */
interface SysDictItem : IManagedDbEntity<String, SysDictItem> {

    companion object : DbEntityFactory<SysDictItem>()

    /** Dictionary item code */
    @get:Sortable
    var itemCode: String

    /** Dictionary item name */
    @get:Sortable
    var itemName: String

    /** Dictionary id */
    var dictId: String

    /** Dictionary item order */
    var orderNum: Int?

    /** Parent id */
    var parentId: String?

}
