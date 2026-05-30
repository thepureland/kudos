package io.kudos.ms.auth.core.role.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.auth.core.role.model.table.AuthRoles
import org.springframework.stereotype.Repository


/**
 * Role DAO.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class AuthRoleDao : BaseCrudDao<String, AuthRole, AuthRoles>() {



    /**
     * Queries all roles with active=true.
     *
     * @return List<AuthRoleCacheEntry>
     */
    open fun searchActiveRolesForCache(): List<AuthRoleCacheEntry> {
        val criteria = Criteria(AuthRole::active eq true)
        return searchAs<AuthRoleCacheEntry>(criteria)
    }

    /**
     * Queries by tenant and role code (regardless of active state) and returns a single cache VO.
     *
     * @param tenantId tenant id
     * @param code role code
     * @return AuthRoleCacheEntry, or null when not found
     */
    open fun searchRoleByTenantIdAndRoleCode(tenantId: String, code: String): AuthRoleCacheEntry? {
        val criteria = Criteria.and(
            AuthRole::tenantId eq tenantId,
            AuthRole::code eq code
        )
        return searchAs<AuthRoleCacheEntry>(criteria).firstOrNull()
    }

    /**
     * Queries all active role IDs under a tenant.
     *
     * @param tenantId tenant id
     * @return list of role IDs
     */
    fun searchActiveRoleIdsByTenantId(tenantId: String): List<String> {
        val criteria = Criteria(AuthRole::tenantId eq tenantId)
            .addAnd(AuthRole::active eq true)
        return searchProperty(criteria, AuthRole::id).filterNotNull()
    }


}
