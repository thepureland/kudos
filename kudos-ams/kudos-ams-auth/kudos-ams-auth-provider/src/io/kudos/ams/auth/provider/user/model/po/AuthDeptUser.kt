package io.kudos.ams.auth.provider.user.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * 部门-用户关系数据库实体
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface AuthDeptUser : IDbEntity<String, AuthDeptUser> {
//endregion your codes 1

    companion object : DbEntityFactory<AuthDeptUser>()

    /** 部门id */
    var deptId: String

    /** 用户id */
    var userId: String

    /** 是否为部门管理员 */
    var deptAdmin: Boolean

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
