package io.kudos.ms.auth.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.common.vo.group.AuthGroupCacheEntry
import io.kudos.ms.auth.core.model.po.AuthGroup
import io.kudos.ms.auth.core.model.table.AuthGroups
import org.springframework.stereotype.Repository


/**
 * 用户组数据访问对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
open class AuthGroupDao : BaseCrudDao<String, AuthGroup, AuthGroups>() {


    /**
     * 查询所有 active=true 的用户组
     *
     * @return List<AuthGroupCacheEntry>
     */
    open fun searchActiveGroupsForCache(): List<AuthGroupCacheEntry> {
        val criteria = Criteria(AuthGroup::active eq true)
        return searchAs(criteria)
    }

    /**
     * 按租户、用户组编码查询（不区分 active），返回单条缓存用 VO
     */
    open fun searchGroupByTenantIdAndGroupCode(tenantId: String, code: String): AuthGroupCacheEntry? {
        val criteria = Criteria.and(
            AuthGroup::tenantId eq tenantId,
            AuthGroup::code eq code
        )
        return searchAs<AuthGroupCacheEntry>(criteria).firstOrNull()
    }


}
