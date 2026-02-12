package io.kudos.ms.user.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
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
        val criteria = Criteria(UserOrg::tenantId eq tenantId)
        return searchAs<UserOrgCacheItem>(criteria)
    }

    /**
     * 查询启用状态下，指定父机构的直接子机构ID列表
     *
     * @param parentId 父机构ID
     * @return 直接子机构ID列表
     */
    fun searchActiveChildOrgIds(parentId: String): List<String> {
        val criteria = Criteria(UserOrg::parentId eq parentId)
            .addAnd(UserOrg::active eq true)
        return searchProperty(criteria, UserOrg::id).filterNotNull()
    }

    /**
     * 查询租户下启用机构；parentId 为 null 时查询全部启用机构
     *
     * @param tenantId 租户ID
     * @param parentId 父机构ID，为null时不按父机构过滤
     * @return 机构列表
     */
    fun searchActiveOrgsByTenantId(tenantId: String, parentId: String? = null): List<UserOrg> {
        val criteria = Criteria(UserOrg::tenantId eq tenantId)
            .addAnd(UserOrg::active eq true)
        if (parentId != null) {
            criteria.addAnd(UserOrg::parentId eq parentId)
        }
        return search(criteria)
    }

    //endregion your codes 2

}
