package io.kudos.ms.user.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.user.common.vo.org.UserOrgCacheItem
import io.kudos.ms.user.core.model.po.UserOrg
import io.kudos.ms.user.core.model.table.UserOrgs
import org.springframework.stereotype.Repository


/**
 * 机构数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class UserOrgDao : BaseCrudDao<String, UserOrg, UserOrgs>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 按租户ID查询，返回缓存用 VO 列表
     *
     * @param tenantId 租户ID
     * @return List<UserOrgCacheItem>
     */
    open fun searchOrgsByTenantIdForCache(tenantId: String): List<UserOrgCacheItem> {
        val criteria = Criteria(UserOrg::tenantId.name, OperatorEnum.EQ, tenantId)
        return searchAs<UserOrgCacheItem>(criteria)
    }

    //endregion your codes 2

}
