package io.kudos.ms.auth.core.role.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * Role-User relation database table-entity association object
 *
 * @author K
 * @author AI: Cursor
 * @author AI: Claude
 * @since 1.0.0
 */
object AuthRoleUsers : StringIdTable<AuthRoleUser>("auth_role_user") {

    /** Role id */
    var roleId = varchar("role_id").bindTo { it.roleId }

    /** User id */
    var userId = varchar("user_id").bindTo { it.userId }

    /** Subject kind: USER / SERVICE / API_KEY. */
    var principalType = varchar("principal_type").bindTo { it.principalType }

    /** Grant effective time; NULL = effective immediately. */
    var startTime = datetime("start_time").bindTo { it.startTime }

    /** Grant expiry time; NULL = never expires. */
    var endTime = datetime("end_time").bindTo { it.endTime }

    /** Who exercised their authority to create this grant. */
    var grantedBy = varchar("granted_by").bindTo { it.grantedBy }

    /** Upstream grant this was delegated from; NULL = direct grant. */
    var parentGrantId = varchar("parent_grant_id").bindTo { it.parentGrantId }

    /** Remaining delegation hops; 0 = terminal. */
    var delegableDepth = int("delegable_depth").bindTo { it.delegableDepth }

    /** Scope frozen at grant time (JSON). */
    var scopeSnapshot = varchar("scope_snapshot").bindTo { it.scopeSnapshot }

    /** Soft revocation flag. */
    var revoked = boolean("revoked").bindTo { it.revoked }

    /** Why the grant was revoked. */
    var revokeReason = varchar("revoke_reason").bindTo { it.revokeReason }

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
