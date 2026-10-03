package io.kudos.ms.auth.core.organization.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.core.organization.model.po.AuthOrganizationAdmin
import io.kudos.ms.auth.core.organization.model.table.AuthOrganizationAdmins
import org.springframework.stereotype.Repository

/**
 * DAO for organization administrator designations.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Repository
open class AuthOrganizationAdminDao : BaseCrudDao<String, AuthOrganizationAdmin, AuthOrganizationAdmins>() {

    open fun searchByOrganization(organizationId: String): List<AuthOrganizationAdmin> =
        search(Criteria(AuthOrganizationAdmin::organizationId eq organizationId))

    open fun find(organizationId: String, userId: String): AuthOrganizationAdmin? =
        search(Criteria.and(AuthOrganizationAdmin::organizationId eq organizationId, AuthOrganizationAdmin::userId eq userId))
            .firstOrNull()

    open fun deleteByUser(userId: String): Int =
        batchDeleteCriteria(Criteria(AuthOrganizationAdmin::userId eq userId))
}
