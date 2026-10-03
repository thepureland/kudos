package io.kudos.ms.auth.core.organization.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.auth.core.organization.model.po.AuthOrganizationAudit
import io.kudos.ms.auth.core.organization.model.table.AuthOrganizationAudits
import org.springframework.stereotype.Repository

/**
 * DAO for organization authorization audit records.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Repository
open class AuthOrganizationAuditDao : BaseCrudDao<String, AuthOrganizationAudit, AuthOrganizationAudits>()
