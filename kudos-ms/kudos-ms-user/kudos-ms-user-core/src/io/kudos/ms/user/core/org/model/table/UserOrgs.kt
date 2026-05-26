package io.kudos.ms.user.core.org.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.user.core.org.model.po.UserOrg
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * Organization database table-entity binding object.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
object UserOrgs : StringIdTable<UserOrg>("user_org") {

    /** Organization name. */
    var name = varchar("name").bindTo { it.name }

    /** Organization short name. */
    var shortName = varchar("short_name").bindTo { it.shortName }

    /** Tenant id. */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** Parent organization id. */
    var parentId = varchar("parent_id").bindTo { it.parentId }

    /** Organization type dictionary code. */
    var orgTypeDictCode = varchar("org_type_dict_code").bindTo { it.orgTypeDictCode }

    /** Sort number. */
    var sortNum = int("sort_num").bindTo { it.sortNum }

    /** Remark. */
    var remark = varchar("remark").bindTo { it.remark }

    /** Whether active. */
    var active = boolean("active").bindTo { it.active }

    /** Whether built-in. */
    var builtIn = boolean("built_in").bindTo { it.builtIn }

    /** Creator id. */
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }

    /** Creator name. */
    var createUserName = varchar("create_user_name").bindTo { it.createUserName }

    /** Create time. */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** Updater id. */
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }

    /** Updater name. */
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }

    /** Update time. */
    var updateTime = datetime("update_time").bindTo { it.updateTime }




}
