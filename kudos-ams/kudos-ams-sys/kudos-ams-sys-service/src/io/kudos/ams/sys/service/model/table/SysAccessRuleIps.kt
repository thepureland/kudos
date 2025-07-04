package io.kudos.ams.sys.service.model.table

import io.kudos.ams.sys.service.model.po.SysAccessRuleIp
import org.ktorm.schema.*
import io.kudos.ability.data.rdb.ktorm.support.StringIdTable


/**
 * ip访问规则数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object SysAccessRuleIps : StringIdTable<SysAccessRuleIp>("sys_access_rule_ip") {
//endregion your codes 1

    /** ip起 */
    var ipStart = long("ip_start").bindTo { it.ipStart }

    /** ip止 */
    var ipEnd = long("ip_end").bindTo { it.ipEnd }

    /** ip类型 */
    var ipType = int("ip_type").bindTo { it.ipType }

    /** 过期时间 */
    var expirationDate = datetime("expiration_date").bindTo { it.expirationDate }

    /** 父规则id */
    var parentRuleId = varchar("parent_rule_id").bindTo { it.parentRuleId }

    /** 备注 */
    var remark = varchar("remark").bindTo { it.remark }

    /** 是否启用 */
    var active = boolean("active").bindTo { it.active }

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