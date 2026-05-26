package io.kudos.ms.auth.core.role.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Role-Resource relation database entity
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface AuthRoleResource : IDbEntity<String, AuthRoleResource> {

    companion object : DbEntityFactory<AuthRoleResource>()

    /** Role id */
    var roleId: String

    /** Resource id */
    var resourceId: String

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
