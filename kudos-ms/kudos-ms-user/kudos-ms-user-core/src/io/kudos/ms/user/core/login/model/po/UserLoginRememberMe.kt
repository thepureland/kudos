package io.kudos.ms.user.core.login.model.po
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
interface UserLoginRememberMe : IDbEntity<String, UserLoginRememberMe> {

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




}
