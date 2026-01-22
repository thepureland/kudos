package io.kudos.ams.auth.common.api

import io.kudos.ams.auth.common.vo.role.AuthRoleCacheItem


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

    //endregion your codes 2

}
