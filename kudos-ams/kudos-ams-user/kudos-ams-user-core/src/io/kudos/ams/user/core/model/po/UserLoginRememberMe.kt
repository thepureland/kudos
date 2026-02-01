package io.kudos.ams.user.core.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * 记住我登录数据库实体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
interface UserLoginRememberMe : IDbEntity<String, UserLoginRememberMe> {
//endregion your codes 1

    companion object : DbEntityFactory<UserLoginRememberMe>()

    /** 用户ID */
    var userId: String

    /** 租户ID */
    var tenantId: String

    /** 用户名 */
    var username: String

    /** 令牌 */
    var token: String?

    /** 最后使用时间 */
    var lastUsed: LocalDateTime?


    //region your codes 2

    //endregion your codes 2

}
