package io.kudos.ms.auth.core.organization.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.core.organization.model.po.AuthManagementRoleGrant
import io.kudos.ms.auth.core.organization.model.table.AuthManagementRoleGrants
import org.springframework.stereotype.Repository

/**
 * DAO for built-in management role grants.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Repository
open class AuthManagementRoleGrantDao : BaseCrudDao<String, AuthManagementRoleGrant, AuthManagementRoleGrants>() {

    open fun searchByOrganization(organizationId: String): List<AuthManagementRoleGrant> =
        search(Criteria(AuthManagementRoleGrant::organizationId eq organizationId))

    open fun searchByUser(organizationId: String, userId: String): List<AuthManagementRoleGrant> =
        search(Criteria.and(AuthManagementRoleGrant::organizationId eq organizationId, AuthManagementRoleGrant::userId eq userId))

    open fun deleteByTenant(tenantId: String): Int =
        batchDeleteCriteria(Criteria(AuthManagementRoleGrant::tenantId eq tenantId))

    open fun deleteByUser(userId: String): Int =
        batchDeleteCriteria(Criteria(AuthManagementRoleGrant::userId eq userId))
}
