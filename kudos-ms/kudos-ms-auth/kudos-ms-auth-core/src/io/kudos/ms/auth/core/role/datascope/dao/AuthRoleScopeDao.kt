package io.kudos.ms.auth.core.role.datascope.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.inList
import io.kudos.ms.auth.core.role.datascope.model.po.AuthRoleScope
import io.kudos.ms.auth.core.role.datascope.model.table.AuthRoleScopes
import org.springframework.stereotype.Repository


/**
 * Role data-scope grant DAO.
 *
 * Every query is dimension-qualified. Values are only comparable within a dimension — an org id and
 * a region code may collide as strings and mean nothing to each other — so a query that forgot the
 * dimension would silently mix them.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Repository
open class AuthRoleScopeDao : BaseCrudDao<String, AuthRoleScope, AuthRoleScopes>() {

    /**
     * The values a role is granted on one dimension.
     *
     * @param roleId role id
     * @param dimension the dimension to read
     * @return the granted values (trimmed)
     */
    open fun searchValuesByRoleId(roleId: String, dimension: String = DIMENSION_ORG): Set<String> {
        val criteria = Criteria.and(
            AuthRoleScope::roleId eq roleId,
            AuthRoleScope::dimension eq dimension,
        )
        return searchProperty(criteria, AuthRoleScope::scopeValue).map { it.trim() }.toSet()
    }

    /**
     * The union of values granted to any of the given roles on one dimension.
     *
     * @param roleIds role ids
     * @param dimension the dimension to read
     * @return the granted values (trimmed); empty when [roleIds] is empty
     */
    open fun searchValuesByRoleIds(
        roleIds: Collection<String>,
        dimension: String = DIMENSION_ORG,
    ): Set<String> {
        if (roleIds.isEmpty()) return emptySet()
        val criteria = Criteria.and(
            AuthRoleScope::roleId inList roleIds.toList(),
            AuthRoleScope::dimension eq dimension,
        )
        return searchProperty(criteria, AuthRoleScope::scopeValue).map { it.trim() }.toSet()
    }

    /**
     * Every dimension a role has grants on, mapped to its values. For the admin view, which shows
     * the whole picture rather than one dimension at a time.
     *
     * @param roleId role id
     * @return dimension -> granted values
     */
    open fun searchAllByRoleId(roleId: String): Map<String, Set<String>> {
        val criteria = Criteria(AuthRoleScope::roleId eq roleId)
        return search(criteria)
            .groupBy { it.dimension.trim() }
            .mapValues { (_, rows) -> rows.map { it.scopeValue.trim() }.toSet() }
    }

    /**
     * Reverse lookup: roles granting a value — impact analysis when the value disappears from the
     * dimension it belongs to.
     *
     * @param dimension the dimension
     * @param scopeValue the value
     * @return role ids
     */
    open fun searchRoleIdsByValue(dimension: String, scopeValue: String): Set<String> {
        val criteria = Criteria.and(
            AuthRoleScope::dimension eq dimension,
            AuthRoleScope::scopeValue eq scopeValue,
        )
        return searchProperty(criteria, AuthRoleScope::roleId).toSet()
    }

    /**
     * Deletes a role's grants on one dimension (the replace-semantics bind).
     *
     * Scoped to a single dimension on purpose: rebinding a role's orgs must not silently wipe the
     * regions somebody configured on a different screen.
     *
     * @param roleId role id
     * @param dimension the dimension to clear
     * @return number of rows deleted
     */
    open fun deleteByRoleId(roleId: String, dimension: String = DIMENSION_ORG): Int {
        val criteria = Criteria.and(
            AuthRoleScope::roleId eq roleId,
            AuthRoleScope::dimension eq dimension,
        )
        return batchDeleteCriteria(criteria)
    }

    /**
     * Deletes every scope grant of a role, across all dimensions.
     *
     * The counterpart to [deleteByRoleId]'s deliberate single-dimension scoping: rebinding one
     * dimension must not touch the others, but *deleting the role* must leave nothing behind. A
     * cascade that cleared only the built-in `org` dimension would strand every application-defined
     * dimension's rows against a role id that no longer exists.
     *
     * @param roleId role id
     * @return number of rows deleted
     */
    open fun deleteAllByRoleId(roleId: String): Int =
        batchDeleteCriteria(Criteria(AuthRoleScope::roleId eq roleId))

    companion object {
        /** The built-in dimension; the only one kudos itself resolves. */
        const val DIMENSION_ORG = "org"
    }
}
