package io.kudos.ms.auth.core.role.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.role.model.po.AuthRole
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * Role database table-entity association object
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
object AuthRoles : StringIdTable<AuthRole>("auth_role") {

    /** Role code */
    var code = varchar("code").bindTo { it.code }

    /** Role name */
    var name = varchar("name").bindTo { it.name }

    /** Tenant id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** Subsystem code */
    var subsysCode = varchar("subsys_code").bindTo { it.subsysCode }

    /** Parent role id (NULL = root role). */
    var parentId = varchar("parent_id").bindTo { it.parentId }

    /** Remark */
    var remark = varchar("remark").bindTo { it.remark }

    /** Whether active */
    var active = boolean("active").bindTo { it.active }

    /** Whether built-in */
    var builtIn = boolean("built_in").bindTo { it.builtIn }

    /** Whether assigning this role requires approval. */
    var approvalRequired = boolean("approval_required").bindTo { it.approvalRequired }

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
