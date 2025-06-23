package io.kudos.ams.sys.service.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * 访问规则数据库实体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface SysAccessRule : IDbEntity<String, SysAccessRule> {
//endregion your codes 1

    companion object : DbEntityFactory<SysAccessRule>()

    /** 租户id */
    var tenantId: String

    /** 子系统编码 */
    var subSystemCode: String?

    /** 门户编码 */
    var portalCode: String

    /** 规则类型 */
    var ruleType: Int

    /** 创建用户 */
    var createUser: String?

    /** 创建时间 */
    var createTime: LocalDateTime?

    /** 更新用户 */
    var updateUser: String?

    /** 更新时间 */
    var updateTime: LocalDateTime?


    //region your codes 2

    //endregion your codes 2

}