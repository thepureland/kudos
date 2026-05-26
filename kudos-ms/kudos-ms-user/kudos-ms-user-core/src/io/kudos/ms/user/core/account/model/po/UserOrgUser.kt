package io.kudos.ms.user.core.account.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Organization-user association database entity.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface UserOrgUser : IDbEntity<String, UserOrgUser> {

    companion object : DbEntityFactory<UserOrgUser>()

    /** Organization id. */
    var orgId: String

    /** User id. */
    var userId: String

    /** Whether the user is an organization admin. */
    var orgAdmin: Boolean

    /** Creator id. */
    var createUserId: String?

    /** Creator name. */
    var createUserName: String?

    /** Create time. */
    var createTime: LocalDateTime?

    /** Updater id. */
    var updateUserId: String?

    /** Updater name. */
    var updateUserName: String?

    /** Update time. */
    var updateTime: LocalDateTime?




}
