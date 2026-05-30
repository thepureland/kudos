package io.kudos.ms.auth.core.role.datascope.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.role.datascope.model.po.AuthRoleOrg
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * Role custom data-scope org grant table-entity association.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object AuthRoleOrgs : StringIdTable<AuthRoleOrg>("auth_role_org") {

    /** Role id */
    var roleId = varchar("role_id").bindTo { it.roleId }

    /** Org id */
    var orgId = varchar("org_id").bindTo { it.orgId }

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
