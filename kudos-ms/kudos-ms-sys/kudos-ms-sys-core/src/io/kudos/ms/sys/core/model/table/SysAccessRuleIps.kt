package io.kudos.ms.sys.core.model.table

import io.kudos.ability.data.rdb.ktorm.support.MaintainableTable
import io.kudos.ms.sys.core.model.po.SysAccessRuleIp
import org.ktorm.schema.datetime
import org.ktorm.schema.long
import org.ktorm.schema.varchar


/**
 * ip访问规则数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object SysAccessRuleIps : MaintainableTable<SysAccessRuleIp>("sys_access_rule_ip") {
//endregion your codes 1

    /** ip起 */
    var ipStart = long("ip_start").bindTo { it.ipStart }

    /** ip止 */
    var ipEnd = long("ip_end").bindTo { it.ipEnd }

    /** ip类型字典代码 */
    var ipTypeDictCode = varchar("ip_type_dict_code").bindTo { it.ipTypeDictCode }

    /** 过期时间 */
    var expirationTime = datetime("expiration_time").bindTo { it.expirationTime }

    /** 父规则id */
    var parentRuleId = varchar("parent_rule_id").bindTo { it.parentRuleId }


    //region your codes 2

    //endregion your codes 2

}