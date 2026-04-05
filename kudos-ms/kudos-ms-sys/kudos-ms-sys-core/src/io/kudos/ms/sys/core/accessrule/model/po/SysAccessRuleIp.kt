package io.kudos.ms.sys.core.accessrule.model.po
import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable
import java.time.LocalDateTime

/**
 * ip访问规则数据库实体
 *
 * @author K
 * @since 1.0.0
 */
interface SysAccessRuleIp : IManagedDbEntity<String, SysAccessRuleIp> {

    companion object : DbEntityFactory<SysAccessRuleIp>()

    @get:Sortable
    override var id: String

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

}