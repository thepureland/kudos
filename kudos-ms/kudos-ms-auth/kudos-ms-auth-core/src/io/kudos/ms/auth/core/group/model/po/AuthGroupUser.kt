package io.kudos.ms.auth.core.group.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Group-user relation database entity.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface AuthGroupUser : IDbEntity<String, AuthGroupUser> {

    companion object : DbEntityFactory<AuthGroupUser>()

    /** Group id. */
    var groupId: String

    /** User id. */
    var userId: String

    /** Creator id. */
    var createUserId: String?

    /** Creator name. */
    var createUserName: String?

    /** Creation time. */
    var createTime: LocalDateTime?

    /** Updater id. */
    var updateUserId: String?

    /** Updater name. */
    var updateUserName: String?

    /** Update time. */
    var updateTime: LocalDateTime?




}
