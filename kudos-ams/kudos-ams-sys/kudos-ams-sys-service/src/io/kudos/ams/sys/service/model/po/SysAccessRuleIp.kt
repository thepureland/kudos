package io.kudos.ams.sys.service.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * ip访问规则数据库实体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface SysAccessRuleIp : IDbEntity<String, SysAccessRuleIp> {
//endregion your codes 1

    companion object : DbEntityFactory<SysAccessRuleIp>()

    /** ip起 */
    var ipStart: Long

    /** ip止 */
    var ipEnd: Long

    /** ip类型 */
    var ipType: Int

    /** 过期时间 */
    var expirationDate: LocalDateTime?

    /** 父规则id */
    var parentRuleId: String

    /** 备注 */
    var remark: String?

    /** 是否启用 */
    var active: Boolean

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