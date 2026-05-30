package io.kudos.ms.auth.core.role.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Role-User relation database entity
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface AuthRoleUser : IDbEntity<String, AuthRoleUser> {

    companion object : DbEntityFactory<AuthRoleUser>()

    /** Role id */
    var roleId: String

    /** User id */
    var userId: String

    /** Grant effective time; NULL = effective immediately. */
    var startTime: LocalDateTime?

    /** Grant expiry time; NULL = never expires. */
    var endTime: LocalDateTime?

    /** Creator id */
    var createUserId: String?

    /** Creator name */
    var createUserName: String?

    /** Create time */
    var createTime: LocalDateTime?

    /** Updater id */
    var updateUserId: String?

    /** Updater name */
    var updateUserName: String?

    /** Update time */
    var updateTime: LocalDateTime?




}
