package io.kudos.ms.auth.core.role.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.role.model.po.AuthRoleResource
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * Role-Resource relation database table-entity association object
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
object AuthRoleResources : StringIdTable<AuthRoleResource>("auth_role_resource") {

    /** Role id */
    var roleId = varchar("role_id").bindTo { it.roleId }

    /** Resource id */
    var resourceId = varchar("resource_id").bindTo { it.resourceId }

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
