package io.kudos.ams.user.core.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * 机构-用户关系数据库实体
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface UserOrgUser : IDbEntity<String, UserOrgUser> {
//endregion your codes 1

    companion object : DbEntityFactory<UserOrgUser>()

    /** 机构id */
    var orgId: String

    /** 用户id */
    var userId: String

    /** 是否为机构管理员 */
    var orgAdmin: Boolean

    /** 创建者id */
    var createUserId: String?

    /** 创建者名称 */
    var createUserName: String?

    /** 创建时间 */
    var createTime: LocalDateTime?

    /** 更新者id */
    var updateUserId: String?

    /** 更新者名称 */
    var updateUserName: String?

    /** 更新时间 */
    var updateTime: LocalDateTime?


    //region your codes 2

    //endregion your codes 2

}
