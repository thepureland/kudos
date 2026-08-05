package io.kudos.ms.auth.core.version.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.version.model.po.AuthPrincipalVersion
import org.ktorm.schema.datetime
import org.ktorm.schema.long
import org.ktorm.schema.varchar


/**
 * Table mapping for [AuthPrincipalVersion].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object AuthPrincipalVersions : StringIdTable<AuthPrincipalVersion>("auth_principal_version") {

    var principalId = varchar("principal_id").bindTo { it.principalId }
    var principalType = varchar("principal_type").bindTo { it.principalType }
    var epoch = long("epoch").bindTo { it.epoch }
    var reason = varchar("reason").bindTo { it.reason }
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }
    var createUserName = varchar("create_user_name").bindTo { it.createUserName }
    var createTime = datetime("create_time").bindTo { it.createTime }
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }
    var updateTime = datetime("update_time").bindTo { it.updateTime }
}
