package io.kudos.ms.sys.core.dict.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.sys.core.dict.model.po.VSysDictItem
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar

/**
 * Dictionary item view (v_sys_dict_item) table-entity binding object, read-only.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
object VSysDictItems : StringIdTable<VSysDictItem>("v_sys_dict_item") {

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

    /** Remark */
    var remark = varchar("remark").bindTo { it.remark }

    /** Whether active */
    var active = boolean("active").bindTo { it.active }

    /** Whether built-in */
    var builtIn = boolean("built_in").bindTo { it.builtIn }

    /** Record creator id */
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }

    /** Record creator name */
    var createUserName = varchar("create_user_name").bindTo { it.createUserName }

    /** Record creation time */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** Record updater id */
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }

    /** Record updater name */
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }

    /** Record update time */
    var updateTime = datetime("update_time").bindTo { it.updateTime }

    /** Dictionary type (from sys_dict) */
    var dictType = varchar("dict_type").bindTo { it.dictType }

    /** Dictionary name (from sys_dict) */
    var dictName = varchar("dict_name").bindTo { it.dictName }

    /** Atomic service code (from sys_dict) */
    var atomicServiceCode = varchar("atomic_service_code").bindTo { it.atomicServiceCode }
}
