package io.kudos.ams.sys.service.model.table

import io.kudos.ams.sys.service.model.po.SysAccessRule
import org.ktorm.schema.*
import io.kudos.ability.data.rdb.ktorm.support.StringIdTable


/**
 * 访问规则数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object SysAccessRules : StringIdTable<SysAccessRule>("sys_access_rule") {
//endregion your codes 1

    /** 租户id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** 子系统编码 */
    var subSystemCode = varchar("sub_system_code").bindTo { it.subSystemCode }

    /** 门户编码 */
    var portalCode = varchar("portal_code").bindTo { it.portalCode }

    /** 规则类型 */
    var ruleType = int("rule_type").bindTo { it.ruleType }

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