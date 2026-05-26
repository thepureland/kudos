package io.kudos.ms.user.core.account.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.user.core.account.model.po.UserOrgUser
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * Org-user relation table-entity binding object
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
object UserOrgUsers : StringIdTable<UserOrgUser>("user_org_user") {

    /** Org id */
    var orgId = varchar("org_id").bindTo { it.orgId }

    /** User id */
    var userId = varchar("user_id").bindTo { it.userId }

    /** Whether the user is an org admin */
    var orgAdmin = boolean("org_admin").bindTo { it.orgAdmin }

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
