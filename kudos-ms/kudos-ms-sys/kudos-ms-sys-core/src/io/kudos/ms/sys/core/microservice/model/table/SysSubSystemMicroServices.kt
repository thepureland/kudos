package io.kudos.ms.sys.core.microservice.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.sys.core.microservice.model.po.SysSubSystemMicroService
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * Sub-system to micro-service relation table-to-entity binding.
 *
 * @author K
 * @since 1.0.0
 */
object SysSubSystemMicroServices : StringIdTable<SysSubSystemMicroService>("sys_sub_system_micro_service") {

    /** Sub-system code */
    var subSystemCode = varchar("sub_system_code").bindTo { it.subSystemCode }

    /** Micro-service code */
    var microServiceCode = varchar("micro_service_code").bindTo { it.microServiceCode }

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




}