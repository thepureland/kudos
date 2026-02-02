package io.kudos.ms.user.common.api

import io.kudos.ms.user.common.vo.org.UserOrgCacheItem
import io.kudos.ms.user.common.vo.user.UserAccountCacheItem


/**
 * 机构 对外API
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface IUserOrgApi {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据id从缓存中获取机构信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param id 机构id
     * @return UserOrgCacheItem, 找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getOrgById(id: String): UserOrgCacheItem?

    /**
     * 根据多个id从缓存中批量获取机构信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param ids 机构id集合
     * @return Map<机构id，UserOrgCacheItem>
     * @author K
     * @since 1.0.0
     */
    fun getOrgsByIds(ids: Collection<String>): Map<String, UserOrgCacheItem>

    /**
     * 根据租户ID从缓存中获取其下所有机构ID，如果缓存中不存在，则从数据库中加载，并回写缓存
     * 只返回active=true的机构ID
     *
     * @param tenantId 租户ID
     * @return List<机构ID>
     * @author K
     * @since 1.0.0
     */
    fun getOrgIds(tenantId: String): List<String>

    /**
     * 根据机构ID获取该机构的所有管理员用户信息
     *
     * @param orgId 机构ID
     * @return List<UserAccountCacheItem> 机构管理员用户列表，如果没有管理员则返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getOrgAdmins(orgId: String): List<UserAccountCacheItem>

    /**
     * 根据机构ID获取该机构下的所有用户列表（包括管理员和普通用户）
     *
     * @param orgId 机构ID
     * @return List<UserAccountCacheItem> 用户列表，如果机构不存在或没有用户则返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getOrgUsers(orgId: String): List<UserAccountCacheItem>

    /**
     * 检查用户是否属于指定机构
     *
     * @param userId 用户ID
     * @param orgId 机构ID
     * @return true表示用户属于该机构，false表示不属于
     * @author K
     * @since 1.0.0
     */
    fun isUserInOrg(userId: String, orgId: String): Boolean

    /**
     * 根据机构ID获取该机构的所有直接子机构列表
     *
     * @param orgId 机构ID
     * @return List<UserOrgCacheItem> 子机构列表，如果没有子机构则返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getChildOrgs(orgId: String): List<UserOrgCacheItem>

    /**
     * 根据机构ID获取该机构的父机构
     *
     * @param orgId 机构ID
     * @return UserOrgCacheItem 父机构，如果没有父机构则返回null
     * @author K
     * @since 1.0.0
     */
    fun getParentOrg(orgId: String): UserOrgCacheItem?

    //endregion your codes 2

}
