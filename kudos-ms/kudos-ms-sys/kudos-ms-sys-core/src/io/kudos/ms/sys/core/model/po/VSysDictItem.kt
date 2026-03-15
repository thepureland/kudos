package io.kudos.ms.sys.core.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * 字典项视图实体（v_sys_dict_item = sys_dict_item left join sys_dict），只读。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface VSysDictItem : IDbEntity<String, VSysDictItem> {

    companion object : DbEntityFactory<VSysDictItem>()

    /** 字典项代码 */
    var itemCode: String

    /** 字典项名称 */
    var itemName: String

    /** 字典id */
    var dictId: String

    /** 字典项排序 */
    var orderNum: Int?

    /** 父id */
    var parentId: String?

    /** 备注 */
    var remark: String?

    /** 是否启用 */
    var active: Boolean

    /** 是否内置 */
    var builtIn: Boolean

    /** 记录创建者id */
    var createUserId: String?

    /** 记录创建者名称 */
    var createUserName: String?

    /** 记录创建时间 */
    var createTime: LocalDateTime?

    /** 记录更新者id */
    var updateUserId: String?

    /** 记录更新者名称 */
    var updateUserName: String?

    /** 记录更新时间 */
    var updateTime: LocalDateTime?

    /** 字典类型（来自 sys_dict） */
    var dictType: String?

    /** 字典名称（来自 sys_dict） */
    var dictName: String?

    /** 原子服务编码（来自 sys_dict） */
    var atomicServiceCode: String?

}
