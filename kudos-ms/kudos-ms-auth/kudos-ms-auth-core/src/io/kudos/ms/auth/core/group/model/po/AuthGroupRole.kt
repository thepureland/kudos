package io.kudos.ms.auth.core.group.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Group-role relation database entity.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface AuthGroupRole : IDbEntity<String, AuthGroupRole> {

    companion object : DbEntityFactory<AuthGroupRole>()

    /** Group id. */
    var groupId: String

    /** Role id. */
    var roleId: String

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
