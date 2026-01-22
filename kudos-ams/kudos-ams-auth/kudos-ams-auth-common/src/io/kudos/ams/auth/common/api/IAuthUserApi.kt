package io.kudos.ams.auth.common.api

import io.kudos.ams.auth.common.vo.user.AuthUserCacheItem
import io.kudos.ams.sys.common.vo.resource.SysResourceCacheItem


/**
 * 用户 对外API
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface IAuthUserApi {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据id从缓存中获取用户信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param id 用户id
     * @return AuthUserCacheItem, 找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getUserById(id: String): AuthUserCacheItem?

    /**
     * 根据多个id从缓存中批量获取用户信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param ids 用户id集合
     * @return Map<用户id，AuthUserCacheItem>
     * @author K
     * @since 1.0.0
     */
    fun getUsersByIds(ids: Collection<String>): Map<String, AuthUserCacheItem>

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
     * 根据用户ID获取该用户有权限访问的资源缓存对象列表
     * 查询流程：用户 → 角色 → 资源（三级关联）
     *
     * @param userId 用户ID
     * @return List<SysResourceCacheItem> 资源缓存对象列表，如果用户不存在或没有资源则返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getResources(userId: String): List<SysResourceCacheItem>

    //endregion your codes 2

}
