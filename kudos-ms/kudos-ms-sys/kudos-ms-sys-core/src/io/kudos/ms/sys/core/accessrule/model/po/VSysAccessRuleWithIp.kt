package io.kudos.ms.sys.core.accessrule.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Ktorm 实体：数据库视图 `v_sys_access_rule_with_ip`（`sys_access_rule` 左连接 `sys_access_rule_ip`），只读。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface VSysAccessRuleWithIp : IDbEntity<String, VSysAccessRuleWithIp> {

    companion object : DbEntityFactory<VSysAccessRuleWithIp>()

    /** 父访问规则 id */
    var parentId: String

    var tenantId: String?

    var tenantName: String?

    var systemCode: String?

    var accessRuleTypeDictCode: String?

    var parentRemark: String?

    var parentActive: Boolean?

    var parentBuiltIn: Boolean?

    var parentCreateUserId: String?

    var parentCreateUserName: String?

    var parentCreateTime: LocalDateTime?

    var parentUpdateUserId: String?

    var parentUpdateUserName: String?

    var parentUpdateTime: LocalDateTime?

    /** sys_access_rule_ip.id */
    var ipId: String?

    /** 来自 `sys_access_rule_ip.ip_start`（NUMERIC，语义同 [SysAccessRuleIp.ipStart]）。 */
    var ipStart: BigDecimal?

    /** 来自 `sys_access_rule_ip.ip_end`。 */
    var ipEnd: BigDecimal?

    var ipTypeDictCode: String?

    var expirationTime: LocalDateTime?

    var parentRuleId: String?

    var remark: String?

    var active: Boolean?

    var builtIn: Boolean?

    var createUserId: String?

    var createUserName: String?

    var createTime: LocalDateTime?

    var updateUserId: String?

    var updateUserName: String?

    var updateTime: LocalDateTime?
}
