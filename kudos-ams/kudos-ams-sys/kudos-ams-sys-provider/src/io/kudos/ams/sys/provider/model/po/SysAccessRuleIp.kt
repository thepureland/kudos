package io.kudos.ams.sys.provider.model.po

import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IMaintainableDbEntity
import java.time.LocalDateTime

/**
 * ip访问规则数据库实体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface SysAccessRuleIp : IMaintainableDbEntity<String, SysAccessRuleIp> {
//endregion your codes 1

    companion object : DbEntityFactory<SysAccessRuleIp>()

    /** ip起 */
    var ipStart: Long

    /** ip止 */
    var ipEnd: Long

    /** ip类型字典代码 */
    var ipTypeDictCode: String

    /** 过期时间 */
    var expirationTime: LocalDateTime?

    /** 父规则id */
    var parentRuleId: String


    //region your codes 2

    //endregion your codes 2

}