package io.kudos.ms.user.common.org.api
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.common.user.vo.UserAccountCacheEntry


/**
 * 机构 对外API
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IUserOrgApi {


    /**
     * 根据id从缓存中获取机构信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param id 机构id
     * @return UserOrgCacheEntry, 找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getOrgById(id: String): UserOrgCacheEntry?

    /**
     * 根据多个id从缓存中批量获取机构信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param ids 机构id集合
     * @return Map<机构id，UserOrgCacheEntry>
     * @author K
     * @since 1.0.0
     */
    fun getOrgsByIds(ids: Collection<String>): Map<String, UserOrgCacheEntry>

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
     * @return List<UserAccountCacheEntry> 机构管理员用户列表，如果没有管理员则返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getOrgAdmins(orgId: String): List<UserAccountCacheEntry>

    /**
     * 根据机构ID获取该机构下的所有用户列表（包括管理员和普通用户）
     *
     * @param orgId 机构ID
     * @return List<UserAccountCacheEntry> 用户列表，如果机构不存在或没有用户则返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getOrgUsers(orgId: String): List<UserAccountCacheEntry>

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
     * @return List<UserOrgCacheEntry> 子机构列表，如果没有子机构则返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getChildOrgs(orgId: String): List<UserOrgCacheEntry>

    /**
     * 根据机构ID获取该机构的父机构
     *
     * @param orgId 机构ID
     * @return UserOrgCacheEntry 父机构，如果没有父机构则返回null
     * @author K
     * @since 1.0.0
     */
    fun getParentOrg(orgId: String): UserOrgCacheEntry?


}
