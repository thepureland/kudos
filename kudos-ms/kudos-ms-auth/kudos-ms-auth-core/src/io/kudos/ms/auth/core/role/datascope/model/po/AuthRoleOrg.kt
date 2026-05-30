package io.kudos.ms.auth.core.role.datascope.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Role custom data-scope org grant (used when the role's data_scope = CUSTOM).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface AuthRoleOrg : IDbEntity<String, AuthRoleOrg> {

    companion object : DbEntityFactory<AuthRoleOrg>()

    /** Role id */
    var roleId: String

    /** Org id the role may access */
    var orgId: String

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
