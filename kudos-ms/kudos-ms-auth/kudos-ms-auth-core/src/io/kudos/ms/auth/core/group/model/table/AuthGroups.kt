package io.kudos.ms.auth.core.group.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.group.model.po.AuthGroup
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * User group table mapping.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object AuthGroups : StringIdTable<AuthGroup>("auth_group") {

    /** Group code. */
    var code = varchar("code").bindTo { it.code }

    /** Group name. */
    var name = varchar("name").bindTo { it.name }

    /** Tenant id. */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** Subsystem code. */
    var subsysCode = varchar("subsys_code").bindTo { it.subsysCode }

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

    /** Creation time. */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** Updater id. */
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }

    /** Updater name. */
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }

    /** Update time. */
    var updateTime = datetime("update_time").bindTo { it.updateTime }




}
