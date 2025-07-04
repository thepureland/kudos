package io.kudos.ams.sys.service.model.table

import io.kudos.ams.sys.service.model.po.SysSubSystemMicroService
import org.ktorm.schema.*
import io.kudos.ability.data.rdb.ktorm.support.StringIdTable


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

    /** 创建用户 */
    var createUser = varchar("create_user").bindTo { it.createUser }

    /** 创建时间 */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** 更新用户 */
    var updateUser = varchar("update_user").bindTo { it.updateUser }

    /** 更新时间 */
    var updateTime = datetime("update_time").bindTo { it.updateTime }


    //region your codes 2

    //endregion your codes 2

}