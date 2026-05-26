package io.kudos.ms.sys.core.dict.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.dict.model.po.SysDictItem
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * Dictionary item database table-entity binding object.
 *
 * @author K
 * @since 1.0.0
 */
object SysDictItems : ManagedTable<SysDictItem>("sys_dict_item") {

    /** Dictionary item code */
    var itemCode = varchar("item_code").bindTo { it.itemCode }

    /** Dictionary item name */
    var itemName = varchar("item_name").bindTo { it.itemName }

    /** Dictionary id */
    var dictId = varchar("dict_id").bindTo { it.dictId }

    /** Dictionary item order */
    var orderNum = int("order_num").bindTo { it.orderNum }

    /** Parent id */
    var parentId = varchar("parent_id").bindTo { it.parentId }




}
