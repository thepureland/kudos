package io.kudos.ms.sys.core.dict.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.sys.core.dict.model.po.VSysDictItem
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar

/**
 * 字典项视图（v_sys_dict_item）表-实体关联对象，只读。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
object VSysDictItems : StringIdTable<VSysDictItem>("v_sys_dict_item") {

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

    /** 备注 */
    var remark = varchar("remark").bindTo { it.remark }

    /** 是否启用 */
    var active = boolean("active").bindTo { it.active }

    /** 是否内置 */
    var builtIn = boolean("built_in").bindTo { it.builtIn }

    /** 记录创建者id */
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }

    /** 记录创建者名称 */
    var createUserName = varchar("create_user_name").bindTo { it.createUserName }

    /** 记录创建时间 */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** 记录更新者id */
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }

    /** 记录更新者名称 */
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }

    /** 记录更新时间 */
    var updateTime = datetime("update_time").bindTo { it.updateTime }

    /** 字典类型（来自 sys_dict） */
    var dictType = varchar("dict_type").bindTo { it.dictType }

    /** 字典名称（来自 sys_dict） */
    var dictName = varchar("dict_name").bindTo { it.dictName }

    /** 原子服务编码（来自 sys_dict） */
    var atomicServiceCode = varchar("atomic_service_code").bindTo { it.atomicServiceCode }
}
