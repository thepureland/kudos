package io.kudos.ms.auth.core.role.datascope.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.inList
import io.kudos.ms.auth.core.role.datascope.model.po.AuthRoleOrg
import io.kudos.ms.auth.core.role.datascope.model.table.AuthRoleOrgs
import org.springframework.stereotype.Repository


/**
 * Role custom data-scope org grant DAO.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Repository
open class AuthRoleOrgDao : BaseCrudDao<String, AuthRoleOrg, AuthRoleOrgs>() {

    /**
     * Returns the org ids granted to a role for CUSTOM data scope.
     *
     * @param roleId role id
     * @return set of org ids (trimmed)
     */
    open fun searchOrgIdsByRoleId(roleId: String): Set<String> {
        val criteria = Criteria(AuthRoleOrg::roleId eq roleId)
        return searchProperty(criteria, AuthRoleOrg::orgId).map { it.trim() }.toSet()
    }

    /**
     * Returns the union of org ids granted to any of the given roles.
     *
     * @param roleIds role ids
     * @return set of org ids (trimmed); empty if [roleIds] is empty
     */
    open fun searchOrgIdsByRoleIds(roleIds: Collection<String>): Set<String> {
        if (roleIds.isEmpty()) return emptySet()
        val criteria = Criteria(AuthRoleOrg::roleId inList roleIds.toList())
        return searchProperty(criteria, AuthRoleOrg::orgId).map { it.trim() }.toSet()
    }

    /**
     * Reverse lookup: role ids that grant the given org (impact analysis when an org is removed).
     *
     * @param orgId org id
     * @return set of role ids
     */
    open fun searchRoleIdsByOrgId(orgId: String): Set<String> {
        val criteria = Criteria(AuthRoleOrg::orgId eq orgId)
        return searchProperty(criteria, AuthRoleOrg::roleId).toSet()
    }

    /**
     * Deletes all org grants for a role (used by the replace-semantics bind).
     *
     * @param roleId role id
     * @return number of rows deleted
     */
    open fun deleteByRoleId(roleId: String): Int {
        val criteria = Criteria(AuthRoleOrg::roleId eq roleId)
        return batchDeleteCriteria(criteria)
    }

}
