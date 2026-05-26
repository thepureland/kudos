package io.kudos.ms.sys.core.dict.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Dictionary item view entity (v_sys_dict_item = sys_dict_item left join sys_dict), read-only.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface VSysDictItem : IDbEntity<String, VSysDictItem> {

    companion object : DbEntityFactory<VSysDictItem>()

    /** Dictionary item code */
    var itemCode: String

    /** Dictionary item name */
    var itemName: String

    /** Dictionary id */
    var dictId: String

    /** Dictionary item order */
    var orderNum: Int?

    /** Parent id */
    var parentId: String?

    /** Remark */
    var remark: String?

    /** Whether active */
    var active: Boolean

    /** Whether built-in */
    var builtIn: Boolean

    /** Record creator id */
    var createUserId: String?

    /** Record creator name */
    var createUserName: String?

    /** Record creation time */
    var createTime: LocalDateTime?

    /** Record updater id */
    var updateUserId: String?

    /** Record updater name */
    var updateUserName: String?

    /** Record update time */
    var updateTime: LocalDateTime?

    /** Dictionary type (from sys_dict) */
    var dictType: String?

    /** Dictionary name (from sys_dict) */
    var dictName: String?

    /** Atomic service code (from sys_dict) */
    var atomicServiceCode: String?

}
