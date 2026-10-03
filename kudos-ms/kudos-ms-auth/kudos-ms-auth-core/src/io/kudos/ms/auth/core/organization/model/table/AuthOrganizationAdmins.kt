package io.kudos.ms.auth.core.organization.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.organization.model.po.AuthOrganizationAdmin
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar

/**
 * Ktorm table mapping for [AuthOrganizationAdmin].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object AuthOrganizationAdmins : StringIdTable<AuthOrganizationAdmin>("auth_organization_admin") {

    var organizationId = varchar("organization_id").bindTo { it.organizationId }

    var userId = varchar("user_id").bindTo { it.userId }

    var createUserId = varchar("create_user_id").bindTo { it.createUserId }

    var createTime = datetime("create_time").bindTo { it.createTime }
}
