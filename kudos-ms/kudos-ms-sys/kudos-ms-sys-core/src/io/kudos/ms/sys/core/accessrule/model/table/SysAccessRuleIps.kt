package io.kudos.ms.sys.core.accessrule.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.accessrule.model.po.SysAccessRuleIp
import org.ktorm.schema.datetime
import org.ktorm.schema.decimal
import org.ktorm.schema.varchar


/**
 * IP access rule table-entity binding object (`sys_access_rule_ip`).
 *
 * `ip_start`/`ip_end` are `NUMERIC(39,0)`, consistent with [io.kudos.ms.sys.core.accessrule.model.po.SysAccessRuleIp].
 *
 * @author K
 * @since 1.0.0
 */
object SysAccessRuleIps : ManagedTable<SysAccessRuleIp>("sys_access_rule_ip") {

    /** Range start (up to 128-bit unsigned). */
    var ipStart = decimal("ip_start").bindTo { it.ipStart }

    /** Range end. */
    var ipEnd = decimal("ip_end").bindTo { it.ipEnd }

    /** IP type dict code */
    var ipTypeDictCode = varchar("ip_type_dict_code").bindTo { it.ipTypeDictCode }

    /** Expiration time */
    var expirationTime = datetime("expiration_time").bindTo { it.expirationTime }

    /** Parent rule id */
    var parentRuleId = varchar("parent_rule_id").bindTo { it.parentRuleId }




}
