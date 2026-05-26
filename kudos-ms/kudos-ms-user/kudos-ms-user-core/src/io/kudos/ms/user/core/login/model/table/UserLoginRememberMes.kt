package io.kudos.ms.user.core.login.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.user.core.login.model.po.UserLoginRememberMe
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * Remember-me login table-entity binding.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object UserLoginRememberMes : StringIdTable<UserLoginRememberMe>("user_login_remember_me") {

    /** User id */
    var userId = varchar("user_id").bindTo { it.userId }

    /** Tenant id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** Username */
    var username = varchar("username").bindTo { it.username }

    /** Token */
    var token = varchar("token").bindTo { it.token }

    /** Last used time */
    var lastUsed = datetime("last_used").bindTo { it.lastUsed }




}
