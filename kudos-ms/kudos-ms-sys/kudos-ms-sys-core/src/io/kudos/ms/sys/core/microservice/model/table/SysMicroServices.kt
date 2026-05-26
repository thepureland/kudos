package io.kudos.ms.sys.core.microservice.model.table

import io.kudos.ms.sys.core.microservice.model.po.SysMicroService
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * Micro-service table-to-entity binding.
 *
 * @author K
 * @since 1.0.0
 */
object SysMicroServices : Table<SysMicroService>("sys_micro_service") {

    /** Code */
    var code = varchar("code").bindTo { it.code }

    /** Name */
    var name = varchar("name").bindTo { it.name }

    /** Context */
    var context = varchar("context").bindTo { it.context }

    /** Whether atomic service */
    var atomicService = boolean("atomic_service").bindTo { it.atomicService }

    /** Parent service code */
    var parentCode = varchar("parent_code").bindTo { it.parentCode }

    /** Remark */
    var remark = varchar("remark").bindTo { it.remark }

    /** Whether active */
    var active = boolean("active").bindTo { it.active }

    /** Whether built-in */
    var builtIn = boolean("built_in").bindTo { it.builtIn }

    /** Creator id */
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }

    /** Creator name */
    var createUserName = varchar("create_user_name").bindTo { it.createUserName }

    /** Create time */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** Updater id */
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }

    /** Updater name */
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }

    /** Update time */
    var updateTime = datetime("update_time").bindTo { it.updateTime }



    /** Primary key alias */
    var id = code.primaryKey()


}
