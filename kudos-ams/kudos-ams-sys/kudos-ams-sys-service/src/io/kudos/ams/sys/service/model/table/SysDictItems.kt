package io.kudos.ams.sys.service.model.table

import io.kudos.ams.sys.service.model.po.SysDictItem
import org.ktorm.schema.*
import io.kudos.ability.data.rdb.ktorm.support.MaintainableTable


/**
 * 字典项数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object SysDictItems : MaintainableTable<SysDictItem>("sys_dict_item") {
//endregion your codes 1

    /** 字典项代码 */
    var itemCode = varchar("item_code").bindTo { it.itemCode }

    /** 字典项名称 */
    var itemName = varchar("item_name").bindTo { it.itemName }

    /** 字典id */
    var dictId = varchar("dict_id").bindTo { it.dictId }

    /** 字典项排序 */
    var orderNum = int("order_num").bindTo { it.orderNum }

    /** 父id */
    var parentId = varchar("parent_id").bindTo { it.parentId }


    //region your codes 2

    //endregion your codes 2

}