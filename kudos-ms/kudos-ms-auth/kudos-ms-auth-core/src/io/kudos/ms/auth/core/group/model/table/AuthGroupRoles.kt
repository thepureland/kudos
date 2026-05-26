package io.kudos.ms.auth.core.group.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.group.model.po.AuthGroupRole
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * Group-role relation table mapping.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object AuthGroupRoles : StringIdTable<AuthGroupRole>("auth_group_role") {

    /** Group id. */
    var groupId = varchar("group_id").bindTo { it.groupId }

    /** Role id. */
    var roleId = varchar("role_id").bindTo { it.roleId }

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
