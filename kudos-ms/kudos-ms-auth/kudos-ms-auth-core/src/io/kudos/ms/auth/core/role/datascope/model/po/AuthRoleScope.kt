package io.kudos.ms.auth.core.role.datascope.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * One value a role may reach on one scope dimension (used when the role's data_scope = CUSTOM).
 *
 * The dimension is data rather than a table name: organization is the common case, but region,
 * brand and project all occur, and an application needing one of those should not have to fork the
 * authorization model. An [io.kudos.ms.auth.core.role.datascope.spi.IScopeDimensionProvider] teaches
 * the resolver how values in a given dimension nest, if they nest at all.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface AuthRoleScope : IDbEntity<String, AuthRoleScope> {

    companion object : DbEntityFactory<AuthRoleScope>()

    /** Role id */
    var roleId: String

    /** The dimension this value belongs to: `org`, `region`, `project` … Registered by the application. */
    var dimension: String

    /** The value itself — an org id for the `org` dimension, whatever the provider defines for others. */
    var scopeValue: String

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
