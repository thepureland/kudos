package io.kudos.ms.user.core.login.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Remember-me login database entity
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface UserLoginRememberMe : IDbEntity<String, UserLoginRememberMe> {

    companion object : DbEntityFactory<UserLoginRememberMe>()

    /** User ID */
    var userId: String

    /** Tenant ID */
    var tenantId: String

    /** Username */
    var username: String

    /** Token */
    var token: String?

    /** Last used time */
    var lastUsed: LocalDateTime?




}
