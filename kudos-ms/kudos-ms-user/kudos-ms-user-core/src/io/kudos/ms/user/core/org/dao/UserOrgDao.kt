package io.kudos.ms.user.core.org.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.core.org.model.po.UserOrg
import io.kudos.ms.user.core.org.model.table.UserOrgs
import org.springframework.stereotype.Repository


/**
 * 机构数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class UserOrgDao : BaseCrudDao<String, UserOrg, UserOrgs>() {


    /**
     * 按租户ID查询，返回缓存用 VO 列表
     *
     * @param tenantId 租户ID
     * @return List<UserOrgCacheEntry>
     */
    open fun searchOrgsByTenantIdForCache(tenantId: String): List<UserOrgCacheEntry> {
        val criteria = Criteria(UserOrg::tenantId eq tenantId)
        return searchAs<UserOrgCacheEntry>(criteria)
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

    /**
     * 返回 rootOrgId 自身 + 所有递归启用子孙机构 ID。
     *
     * 用于"父机构看子机构成员"类查询的 IN-list 展开。仅含 `active=true` 的子机构，
     * 与 [searchActiveChildOrgIds] 的过滤口径一致。
     *
     * 性能：树深度 N 时为 N 次 SELECT。中型机构树（< 几百节点）够用；超大树可换成
     * 单次全量 parentId 反向索引（见 [searchAllOrgIdToParentId]）批量计算。
     *
     * @return Set，含 rootOrgId 自身；rootOrgId 本身未必 active，调用方按需过滤。
     */
    fun searchOrgAndDescendantIds(rootOrgId: String): Set<String> {
        val result = linkedSetOf(rootOrgId)
        val queue = ArrayDeque<String>().apply { add(rootOrgId) }
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            searchActiveChildOrgIds(current).forEach { childId ->
                if (result.add(childId)) queue.add(childId)
            }
        }
        return result
    }

    /**
     * 返回 orgId 自身 + 所有祖先机构 ID（沿 parent_id 上溯到 null 或环路）。
     *
     * 用于缓存失效：orgId 下的用户关系变化时，所有祖先机构的"含子机构成员"视图都要重算。
     * 不过滤 active —— 即便祖先被禁用，也可能存在缓存条目要清。
     */
    fun searchOrgAndAncestorIds(orgId: String): Set<String> {
        val result = linkedSetOf(orgId)
        var current: String? = get(orgId)?.parentId
        while (current != null && result.add(current)) {
            current = get(current)?.parentId
        }
        return result
    }

    /**
     * 全量机构的 orgId → parentId 映射，给缓存批量预热构造父子索引用。
     * value 为 null 表示根机构。
     */
    fun searchAllOrgIdToParentId(): Map<String, String?> {
        return allSearch().associate { it.id to it.parentId }
    }


}
