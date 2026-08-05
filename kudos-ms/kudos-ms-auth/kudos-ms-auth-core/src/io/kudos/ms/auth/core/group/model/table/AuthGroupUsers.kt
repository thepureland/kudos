package io.kudos.ms.auth.core.group.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * Group-user relation table mapping.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
object AuthGroupUsers : StringIdTable<AuthGroupUser>("auth_group_user") {

    /** Group id. */
    var groupId = varchar("group_id").bindTo { it.groupId }

    /** User id. */
    var userId = varchar("user_id").bindTo { it.userId }

    /** Subject kind: USER / SERVICE / API_KEY. */
    var principalType = varchar("principal_type").bindTo { it.principalType }

    /** Membership effective time; NULL = effective immediately. */
    var startTime = datetime("start_time").bindTo { it.startTime }

    /** Membership expiry time; NULL = never expires. */
    var endTime = datetime("end_time").bindTo { it.endTime }

    /** Who put this principal into the group. */
    var grantedBy = varchar("granted_by").bindTo { it.grantedBy }

    /** Upstream grant this membership was delegated from. */
    var parentGrantId = varchar("parent_grant_id").bindTo { it.parentGrantId }

    /** Soft revocation flag. */
    var revoked = boolean("revoked").bindTo { it.revoked }

    /** Why the membership was revoked. */
    var revokeReason = varchar("revoke_reason").bindTo { it.revokeReason }

    /** Creator id. */
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }

    /** Creator name. */
    var createUserName = varchar("create_user_name").bindTo { it.createUserName }

    /** Creation time. */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** Updater id. */
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }

    /** Updater name. */
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }

    /** Update time. */
    var updateTime = datetime("update_time").bindTo { it.updateTime }




}
