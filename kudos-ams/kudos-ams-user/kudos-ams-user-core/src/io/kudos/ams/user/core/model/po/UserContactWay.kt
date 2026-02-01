package io.kudos.ams.user.core.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * 用户联系方式数据库实体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
interface UserContactWay : IDbEntity<String, UserContactWay> {
//endregion your codes 1

    companion object : DbEntityFactory<UserContactWay>()

    /** 用户ID */
    var userId: String?

    /** 联系方式字典码 */
    var contactWayDictCode: String

    /** 联系方式值 */
    var contactWayValue: String

    /** 联系方式状态字典码 */
    var contactWayStatusDictCode: String

    /** 优先级 */
    var priority: Int?

    /** 备注 */
    var remark: String?

    /** 是否启用 */
    var active: Boolean

    /** 是否内置 */
    var builtIn: Boolean

    /** 创建者ID */
    var createUserId: String?

    /** 创建者名称 */
    var createUserName: String?

    /** 创建时间 */
    var createTime: LocalDateTime?

    /** 更新者ID */
    var updateUserId: String?

    /** 更新者名称 */
    var updateUserName: String?

    /** 更新时间 */
    var updateTime: LocalDateTime?


    //region your codes 2

    //endregion your codes 2

}
