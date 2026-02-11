package io.kudos.ms.auth.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.auth.common.vo.group.AuthGroupCacheItem
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
//region your codes 1
open class AuthGroupDao : BaseCrudDao<String, AuthGroup, AuthGroups>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 查询所有 active=true 的用户组
     *
     * @return List<AuthGroupCacheItem>
     */
    open fun searchActiveGroupsForCache(): List<AuthGroupCacheItem> {
        val criteria = Criteria(AuthGroup::active.name, OperatorEnum.EQ, true)
        return searchAs(criteria)
    }

    /**
     * 按租户、用户组编码查询（不区分 active），返回单条缓存用 VO
     */
    open fun searchGroupByTenantIdAndGroupCode(tenantId: String, code: String): AuthGroupCacheItem? {
        val criteria = Criteria.and(
            Criterion(AuthGroup::tenantId.name, OperatorEnum.EQ, tenantId),
            Criterion(AuthGroup::code.name, OperatorEnum.EQ, code)
        )
        return searchAs<AuthGroupCacheItem>(criteria).firstOrNull()
    }

    //endregion your codes 2

}
