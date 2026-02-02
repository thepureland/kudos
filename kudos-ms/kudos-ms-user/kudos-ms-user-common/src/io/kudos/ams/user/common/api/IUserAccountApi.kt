package io.kudos.ms.user.common.api

import io.kudos.ms.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.ms.user.common.vo.org.UserOrgCacheItem
import io.kudos.ms.user.common.vo.user.UserAccountCacheItem


/**
 * 用户 对外API
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface IUserAccountApi {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据id从缓存中获取用户信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param id 用户id
     * @return UserAccountCacheItem, 找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getUserById(id: String): UserAccountCacheItem?

    /**
     * 根据多个id从缓存中批量获取用户信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param ids 用户id集合
     * @return Map<用户id，UserAccountCacheItem>
     * @author K
     * @since 1.0.0
     */
    fun getUsersByIds(ids: Collection<String>): Map<String, UserAccountCacheItem>

    /**
     * 根据租户ID和用户名从缓存获取对应的用户ID，如果缓存中不存在，则从数据库中加载，并写回缓存
     * 只返回active=true的用户ID
     *
     * @param tenantId 租户ID
     * @param username 用户名
     * @return 用户ID，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getUserId(tenantId: String, username: String): String?



    /**
     * 根据用户ID获取该用户所属的所有机构列表
     *
     * @param userId 用户ID
     * @return List<UserOrgCacheItem> 机构列表，如果用户不存在或没有机构则返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getUserOrgs(userId: String): List<UserOrgCacheItem>


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
     * 根据租户ID获取该租户下所有激活用户的ID列表
     * 只返回active=true的用户ID
     *
     * @param tenantId 租户ID
     * @return List<String> 用户ID列表
     * @author K
     * @since 1.0.0
     */
    fun getUserIds(tenantId: String): List<String>

    //endregion your codes 2

}
