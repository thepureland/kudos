package io.kudos.ams.sys.core.model.table

import io.kudos.ams.sys.core.model.po.SysMicroServiceAtomicService
import org.ktorm.schema.*
import io.kudos.ability.data.rdb.ktorm.support.StringIdTable


/**
 * 微服务-原子服务关系数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object SysMicroServiceAtomicServices : StringIdTable<SysMicroServiceAtomicService>("sys_micro_service_atomic_service") {
//endregion your codes 1

    /** 微服务编码 */
    var microServiceCode = varchar("micro_service_code").bindTo { it.microServiceCode }

    /** 原子服务编码 */
    var atomicServiceCode = varchar("atomic_service_code").bindTo { it.atomicServiceCode }

    /** 创建者id */
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }

    /** 创建者名称 */
    var createUserName = varchar("create_user_name").bindTo { it.createUserName }

    /** 创建时间 */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** 更新者id */
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }

    /** 更新者名称 */
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }

    /** 更新时间 */
    var updateTime = datetime("update_time").bindTo { it.updateTime }


    //region your codes 2

    //endregion your codes 2

}