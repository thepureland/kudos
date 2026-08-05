package io.kudos.ms.auth.core.role.datascope.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.role.datascope.model.po.AuthRoleScope
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * Role custom data-scope org grant table-entity association.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object AuthRoleScopes : StringIdTable<AuthRoleScope>("auth_role_scope") {

    /** Role id */
    var roleId = varchar("role_id").bindTo { it.roleId }

    /** Scope dimension. */
    var dimension = varchar("dimension").bindTo { it.dimension }

    /** The granted value on that dimension. */
    var scopeValue = varchar("scope_value").bindTo { it.scopeValue }

    /** Creator id */
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }

    /** Creator name */
    var createUserName = varchar("create_user_name").bindTo { it.createUserName }

    /** Create time */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** Updater id */
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }

    /** Updater name */
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }

    /** Update time */
    var updateTime = datetime("update_time").bindTo { it.updateTime }

}
