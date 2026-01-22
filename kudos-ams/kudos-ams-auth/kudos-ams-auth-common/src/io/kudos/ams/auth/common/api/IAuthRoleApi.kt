package io.kudos.ams.auth.common.api

import io.kudos.ams.auth.common.vo.role.AuthRoleCacheItem
import io.kudos.ams.auth.common.vo.user.AuthUserCacheItem
import io.kudos.ams.sys.common.vo.resource.SysResourceCacheItem


/**
 * 角色 对外API
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface IAuthRoleApi {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据id从缓存中获取角色信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param id 角色id
     * @return AuthRoleCacheItem, 找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getRoleById(id: String): AuthRoleCacheItem?

    /**
     * 根据多个id从缓存中批量获取角色信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param ids 角色id集合
     * @return Map<角色id，AuthRoleCacheItem>
     * @author K
     * @since 1.0.0
     */
    fun getRolesByIds(ids: Collection<String>): Map<String, AuthRoleCacheItem>

    /**
     * 根据租户ID和角色编码从缓存获取对应的角色ID，如果缓存中不存在，则从数据库中加载，并写回缓存
     * 只返回active=true的角色ID
     *
     * @param tenantId 租户ID
     * @param code 角色编码
     * @return 角色ID，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getRoleId(tenantId: String, code: String): String?

    /**
     * 根据角色ID获取拥有该角色的所有用户列表
     *
     * @param roleId 角色ID
     * @return List<AuthUserCacheItem> 用户列表，如果角色不存在或没有用户则返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getRoleUsers(roleId: String): List<AuthUserCacheItem>

    /**
     * 根据租户ID和角色编码获取拥有该角色的所有用户ID列表
     *
     * @param tenantId 租户ID
     * @param roleCode 角色编码
     * @return List<String> 用户ID列表，如果角色不存在或没有用户则返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getUserIdsByRoleCode(tenantId: String, roleCode: String): List<String>

    /**
     * 根据角色ID获取该角色拥有的所有资源列表
     *
     * @param roleId 角色ID
     * @return List<SysResourceCacheItem> 资源列表，如果角色不存在或没有资源则返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getRoleResources(roleId: String): List<SysResourceCacheItem>

    /**
     * 检查角色是否拥有指定资源
     *
     * @param roleId 角色ID
     * @param resourceId 资源ID
     * @return true表示角色拥有该资源，false表示不拥有
     * @author K
     * @since 1.0.0
     */
    fun hasResource(roleId: String, resourceId: String): Boolean

    /**
     * 根据租户ID获取该租户下所有激活角色的ID列表
     * 只返回active=true的角色ID
     *
     * @param tenantId 租户ID
     * @return List<String> 角色ID列表
     * @author K
     * @since 1.0.0
     */
    fun getRoleIds(tenantId: String): List<String>

    //endregion your codes 2

}
