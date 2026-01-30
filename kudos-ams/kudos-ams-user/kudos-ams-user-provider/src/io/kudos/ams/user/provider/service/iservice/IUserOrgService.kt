package io.kudos.ams.user.provider.service.iservice

import io.kudos.ams.user.common.vo.org.UserOrgCacheItem
import io.kudos.ams.user.common.vo.org.UserOrgTreeRecord
import io.kudos.ams.user.common.vo.user.UserAccountCacheItem
import io.kudos.ams.user.provider.model.po.UserOrg
import io.kudos.base.support.iservice.IBaseCrudService


/**
 * 机构业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface IUserOrgService : IBaseCrudService<String, UserOrg> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据机构ID获取该机构的所有管理员用户信息
     *
     * @param orgId 机构ID
     * @return List<UserAccountCacheItem> 机构管理员用户列表，如果没有管理员则返回空列表
     */
    fun getOrgAdmins(orgId: String): List<UserAccountCacheItem>

    /**
     * 根据机构ID获取该机构下的所有用户ID列表（包括管理员和普通用户）
     *
     * @param orgId 机构ID
     * @return List<String> 用户ID列表，如果机构不存在或没有用户则返回空列表
     */
    fun getOrgUserIds(orgId: String): List<String>

    /**
     * 根据机构ID获取该机构的所有直接子机构ID列表
     *
     * @param orgId 机构ID
     * @return List<String> 子机构ID列表，如果没有子机构则返回空列表
     */
    fun getChildOrgIds(orgId: String): List<String>

    /**
     * 根据机构ID获取该机构下的所有用户列表（包括管理员和普通用户）
     *
     * @param orgId 机构ID
     * @return List<UserAccountCacheItem> 用户列表，如果机构不存在或没有用户则返回空列表
     */
    fun getOrgUsers(orgId: String): List<UserAccountCacheItem>

    /**
     * 检查用户是否属于指定机构
     *
     * @param userId 用户ID
     * @param orgId 机构ID
     * @return true表示用户属于该机构，false表示不属于
     */
    fun isUserInOrg(userId: String, orgId: String): Boolean

    /**
     * 根据机构ID获取该机构的所有直接子机构列表
     *
     * @param orgId 机构ID
     * @return List<UserOrgCacheItem> 子机构列表，如果没有子机构则返回空列表
     */
    fun getChildOrgs(orgId: String): List<UserOrgCacheItem>

    /**
     * 根据机构ID获取该机构的父机构
     *
     * @param orgId 机构ID
     * @return UserOrgCacheItem 父机构，如果没有父机构则返回null
     */
    fun getParentOrg(orgId: String): UserOrgCacheItem?

    /**
     * 根据ID获取机构记录（从缓存）
     *
     * @param id 机构ID
     * @return 机构缓存项，找不到返回null
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getOrgRecord(id: String): UserOrgCacheItem?

    /**
     * 根据租户ID获取机构列表
     *
     * @param tenantId 租户ID
     * @return 机构缓存项列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getOrgsByTenantId(tenantId: String): List<UserOrgCacheItem>

    /**
     * 获取机构树形结构
     *
     * @param tenantId 租户ID
     * @param parentId 父机构ID，为null时返回顶级机构
     * @return 机构树节点列表（树形结构，包含children字段）
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getOrgTree(tenantId: String, parentId: String? = null): List<UserOrgTreeRecord>

    /**
     * 获取所有祖先机构ID列表（向上递归）
     *
     * @param orgId 机构ID
     * @return 祖先机构ID列表（从直接父机构到根机构）
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getAllAncestorOrgIds(orgId: String): List<String>

    /**
     * 获取所有后代机构ID列表（向下递归）
     *
     * @param orgId 机构ID
     * @return 后代机构ID列表（包括所有子机构、孙机构等）
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getAllDescendantOrgIds(orgId: String): List<String>

    /**
     * 更新机构启用状态
     *
     * @param id 机构ID
     * @param active 是否启用
     * @return 是否更新成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * 移动机构（调整父机构和排序号）
     *
     * @param id 机构ID
     * @param newParentId 新的父机构ID，为null表示移动到顶级
     * @param newSortNum 新的排序号
     * @return 是否更新成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun moveOrg(id: String, newParentId: String?, newSortNum: Int?): Boolean

    //endregion your codes 2

}
