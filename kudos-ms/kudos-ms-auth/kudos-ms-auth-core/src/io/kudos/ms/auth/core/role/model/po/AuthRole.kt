package io.kudos.ms.auth.core.role.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Role database entity
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface AuthRole : IDbEntity<String, AuthRole> {

    companion object : DbEntityFactory<AuthRole>()

    /** Role code */
    var code: String

    /** Role name */
    var name: String

    /** Tenant id */
    var tenantId: String

    /** Subsystem code */
    var subsysCode: String

    /** Parent role id (NULL = root). Children inherit the parent's resource grants. */
    var parentId: String?

    /** Remark */
    var remark: String?

    /** Whether active */
    var active: Boolean

    /** Whether built-in */
    var builtIn: Boolean?

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
