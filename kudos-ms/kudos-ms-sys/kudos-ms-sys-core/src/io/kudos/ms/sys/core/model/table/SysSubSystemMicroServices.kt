package io.kudos.ms.sys.core.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.sys.core.model.po.SysSubSystemMicroService
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * 子系统-微服务关系数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object SysSubSystemMicroServices : StringIdTable<SysSubSystemMicroService>("sys_sub_system_micro_service") {
//endregion your codes 1

    /** 子系统编码 */
    var subSystemCode = varchar("sub_system_code").bindTo { it.subSystemCode }

    /** 微服务编码 */
    var microServiceCode = varchar("micro_service_code").bindTo { it.microServiceCode }

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