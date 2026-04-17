package io.kudos.ms.sys.core.accessrule.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.accessrule.model.po.SysAccessRuleIp
import org.ktorm.schema.datetime
import org.ktorm.schema.decimal
import org.ktorm.schema.varchar


/**
 * ip访问规则数据库表-实体关联对象（`sys_access_rule_ip`）。
 *
 * `ip_start`/`ip_end` 为 `NUMERIC(39,0)`，与 [io.kudos.ms.sys.core.accessrule.model.po.SysAccessRuleIp] 一致。
 *
 * @author K
 * @since 1.0.0
 */
object SysAccessRuleIps : ManagedTable<SysAccessRuleIp>("sys_access_rule_ip") {

    /** 区间起点（最大 128 位无符号）。 */
    var ipStart = decimal("ip_start").bindTo { it.ipStart }

    /** 区间终点。 */
    var ipEnd = decimal("ip_end").bindTo { it.ipEnd }

    /** ip类型字典代码 */
    var ipTypeDictCode = varchar("ip_type_dict_code").bindTo { it.ipTypeDictCode }

    /** 过期时间 */
    var expirationTime = datetime("expiration_time").bindTo { it.expirationTime }

    /** 父规则id */
    var parentRuleId = varchar("parent_rule_id").bindTo { it.parentRuleId }




}
