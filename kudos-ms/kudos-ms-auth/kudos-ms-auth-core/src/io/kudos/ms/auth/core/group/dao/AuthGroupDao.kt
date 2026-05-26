package io.kudos.ms.auth.core.group.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.common.group.vo.AuthGroupCacheEntry
import io.kudos.ms.auth.core.group.model.po.AuthGroup
import io.kudos.ms.auth.core.group.model.table.AuthGroups
import org.springframework.stereotype.Repository


/**
 * User group DAO.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
open class AuthGroupDao : BaseCrudDao<String, AuthGroup, AuthGroups>() {


    /**
     * Returns all user groups with active=true.
     *
     * @return list of AuthGroupCacheEntry
     */
    open fun searchActiveGroupsForCache(): List<AuthGroupCacheEntry> {
        val criteria = Criteria(AuthGroup::active eq true)
        return searchAs(criteria)
    }

    /**
     * Looks up a single cache VO by tenant and group code (regardless of active flag).
     */
    open fun searchGroupByTenantIdAndGroupCode(tenantId: String, code: String): AuthGroupCacheEntry? {
        val criteria = Criteria.and(
            AuthGroup::tenantId eq tenantId,
            AuthGroup::code eq code
        )
        return searchAs<AuthGroupCacheEntry>(criteria).firstOrNull()
    }


}
