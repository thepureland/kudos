package io.kudos.ms.auth.core.organization.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.organization.model.po.AuthOrganizationAudit
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar

/**
 * Ktorm table mapping for [AuthOrganizationAudit].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object AuthOrganizationAudits : StringIdTable<AuthOrganizationAudit>("auth_organization_audit") {

    var organizationId = varchar("organization_id").bindTo { it.organizationId }

    var actorId = varchar("actor_id").bindTo { it.actorId }

    var action = varchar("action").bindTo { it.action }

    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    var targetUserId = varchar("target_user_id").bindTo { it.targetUserId }

    var targetId = varchar("target_id").bindTo { it.targetId }

    var detail = varchar("detail").bindTo { it.detail }

    var reason = varchar("reason").bindTo { it.reason }

    var createTime = datetime("create_time").bindTo { it.createTime }
}
