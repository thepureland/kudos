package io.kudos.ms.sys.core.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.sys.core.model.po.VSysAccessRuleWithIp
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.long
import org.ktorm.schema.varchar

/**
 * 视图 `v_sys_access_rule_with_ip` 的 [org.ktorm.schema.Table] 定义，与 [VSysAccessRuleWithIp] 绑定。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
object VSysAccessRuleWithIps : StringIdTable<VSysAccessRuleWithIp>("v_sys_access_rule_with_ip") {

    var parentId = varchar("parent_id").bindTo { it.parentId }

    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    var systemCode = varchar("system_code").bindTo { it.systemCode }

    var accessRuleTypeDictCode = varchar("access_rule_type_dict_code").bindTo { it.accessRuleTypeDictCode }

    var parentRemark = varchar("parent_remark").bindTo { it.parentRemark }

    var parentActive = boolean("parent_active").bindTo { it.parentActive }

    var parentBuiltIn = boolean("parent_built_in").bindTo { it.parentBuiltIn }

    var parentCreateUserId = varchar("parent_create_user_id").bindTo { it.parentCreateUserId }

    var parentCreateUserName = varchar("parent_create_user_name").bindTo { it.parentCreateUserName }

    var parentCreateTime = datetime("parent_create_time").bindTo { it.parentCreateTime }

    var parentUpdateUserId = varchar("parent_update_user_id").bindTo { it.parentUpdateUserId }

    var parentUpdateUserName = varchar("parent_update_user_name").bindTo { it.parentUpdateUserName }

    var parentUpdateTime = datetime("parent_update_time").bindTo { it.parentUpdateTime }

    var ipId = varchar("ip_id").bindTo { it.ipId }

    var ipStart = long("ip_start").bindTo { it.ipStart }

    var ipEnd = long("ip_end").bindTo { it.ipEnd }

    var ipTypeDictCode = varchar("ip_type_dict_code").bindTo { it.ipTypeDictCode }

    var expirationTime = datetime("expiration_time").bindTo { it.expirationTime }

    var parentRuleId = varchar("parent_rule_id").bindTo { it.parentRuleId }

    var remark = varchar("remark").bindTo { it.remark }

    var active = boolean("active").bindTo { it.active }

    var builtIn = boolean("built_in").bindTo { it.builtIn }

    var createUserId = varchar("create_user_id").bindTo { it.createUserId }

    var createUserName = varchar("create_user_name").bindTo { it.createUserName }

    var createTime = datetime("create_time").bindTo { it.createTime }

    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }

    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }

    var updateTime = datetime("update_time").bindTo { it.updateTime }
}
