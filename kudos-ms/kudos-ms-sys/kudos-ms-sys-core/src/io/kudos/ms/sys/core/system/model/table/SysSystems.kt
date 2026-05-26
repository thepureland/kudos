package io.kudos.ms.sys.core.system.model.table

import io.kudos.ms.sys.core.system.model.po.SysSystem
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * System table-entity binding object.
 *
 * @author K
 * @since 1.0.0
 */
object SysSystems : Table<SysSystem>("sys_system") {

    /** Code */
    var code = varchar("code").bindTo { it.code }

    /** Name */
    var name = varchar("name").bindTo { it.name }

    /** Whether it is a sub-system */
    var subSystem = boolean("sub_system").bindTo { it.subSystem }

    /** Parent system code */
    var parentCode = varchar("parent_code").bindTo { it.parentCode }

    /** Remark */
    var remark = varchar("remark").bindTo { it.remark }

    /** Whether enabled */
    var active = boolean("active").bindTo { it.active }

    /** Whether built-in */
    var builtIn = boolean("built_in").bindTo { it.builtIn }

    /** Creator id */
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }

    /** Creator name */
    var createUserName = varchar("create_user_name").bindTo { it.createUserName }

    /** Created time */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** Updater id */
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }

    /** Updater name */
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }

    /** Updated time */
    var updateTime = datetime("update_time").bindTo { it.updateTime }



    /** Primary key alias */
    var id = code.primaryKey()


}
