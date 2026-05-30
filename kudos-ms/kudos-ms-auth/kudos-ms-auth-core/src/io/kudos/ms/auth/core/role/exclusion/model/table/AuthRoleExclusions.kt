package io.kudos.ms.auth.core.role.exclusion.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.role.exclusion.model.po.AuthRoleExclusion
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar

/**
 * Ktorm table mapping for [AuthRoleExclusion].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object AuthRoleExclusions : StringIdTable<AuthRoleExclusion>("auth_role_exclusion") {

    var roleAId = varchar("role_a_id").bindTo { it.roleAId }
    var roleBId = varchar("role_b_id").bindTo { it.roleBId }
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var description = varchar("description").bindTo { it.description }
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }
    var createUserName = varchar("create_user_name").bindTo { it.createUserName }
    var createTime = datetime("create_time").bindTo { it.createTime }
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }
    var updateTime = datetime("update_time").bindTo { it.updateTime }
}
