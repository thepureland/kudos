package io.kudos.ms.user.core.org.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.common.org.vo.response.UserOrgTreeRow
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.core.org.model.po.UserOrg


/**
 * 机构业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IUserOrgService : IBaseCrudService<String, UserOrg> {


    /**
     * 根据机构ID获取该机构的所有管理员用户信息
     *
     * @param orgId 机构ID
     * @return List<UserAccountCacheEntry> 机构管理员用户列表，如果没有管理员则返回空列表
     */
    fun getOrgAdmins(orgId: String): List<UserAccountCacheEntry>

    /**
     * 根据机构ID获取该机构及其所有启用子孙机构下的用户ID列表（含管理员和普通用户，去重）。
     *
     * 业务场景："销售总监" 查 "销售部" → 应包含 "销售部/华东" 等子机构的成员。
     * 如果只要直接挂载在该机构的成员，请直接走 [io.kudos.ms.user.core.account.dao.UserOrgUserDao.searchUserIdsByOrgId]。
     *
     * @param orgId 机构ID
     * @return List<String> 用户ID列表，机构不存在或子树为空时返回空列表
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
     * 根据机构ID获取该机构及其所有启用子孙机构下的用户列表（含管理员和普通用户，去重）。
     * 语义等价于 [getOrgUserIds] 转 [UserAccountCacheEntry]。
     *
     * @param orgId 机构ID
     * @return List<UserAccountCacheEntry> 用户列表
     */
    fun getOrgUsers(orgId: String): List<UserAccountCacheEntry>

    /**
     * 检查用户是否属于指定机构或其任意启用子孙机构。
     *
     * 即"用户处于 orgId 子树之内"。如果只要判断"用户直接挂载在 orgId"，
     * 走 [io.kudos.ms.user.core.account.dao.UserOrgUserDao.exists]。
     *
     * @param userId 用户ID
     * @param orgId 机构ID
     * @return true 表示在子树内
     */
    fun isUserInOrg(userId: String, orgId: String): Boolean

    /**
     * 根据机构ID获取该机构的所有直接子机构列表
     *
     * @param orgId 机构ID
     * @return List<UserOrgCacheEntry> 子机构列表，如果没有子机构则返回空列表
     */
    fun getChildOrgs(orgId: String): List<UserOrgCacheEntry>

    /**
     * 根据机构ID获取该机构的父机构
     *
     * @param orgId 机构ID
     * @return UserOrgCacheEntry 父机构，如果没有父机构则返回null
     */
    fun getParentOrg(orgId: String): UserOrgCacheEntry?

    /**
     * 根据ID获取机构记录（从缓存）
     *
     * @param id 机构ID
     * @return 机构缓存项，找不到返回null
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getOrgRecord(id: String): UserOrgCacheEntry?

    /**
     * 根据租户ID获取机构列表
     *
     * @param tenantId 租户ID
     * @return 机构缓存项列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getOrgsByTenantId(tenantId: String): List<UserOrgCacheEntry>

    /**
     * 获取机构树形结构
     *
     * @param tenantId 租户ID
     * @param parentId 父机构ID，为null时返回顶级机构
     * @return 机构树节点列表（树形结构，包含children字段）
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getOrgTree(tenantId: String, parentId: String? = null): List<UserOrgTreeRow>

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


}
