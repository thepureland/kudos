package io.kudos.ms.sys.core.model.table

import io.kudos.ms.sys.core.model.po.SysTenantSystem
import org.ktorm.schema.*
import io.kudos.ability.data.rdb.ktorm.support.StringIdTable


/**
 * 租户-系统关系数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object SysTenantSystems : StringIdTable<SysTenantSystem>("sys_tenant_system") {
//endregion your codes 1

    /** 租户id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** 系统编码 */
    var systemCode = varchar("system_code").bindTo { it.systemCode }

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
