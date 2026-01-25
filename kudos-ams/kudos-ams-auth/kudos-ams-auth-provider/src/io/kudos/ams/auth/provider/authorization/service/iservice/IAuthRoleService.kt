package io.kudos.ams.auth.provider.authorization.service.iservice

import io.kudos.ams.auth.common.vo.role.AuthRoleCacheItem
import io.kudos.ams.auth.common.vo.role.AuthRoleRecord
import io.kudos.ams.auth.common.vo.user.AuthUserCacheItem
import io.kudos.ams.auth.provider.authorization.model.po.AuthRole
import io.kudos.ams.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.base.support.iservice.IBaseCrudService


/**
 * 角色业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface IAuthRoleService : IBaseCrudService<String, AuthRole> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据角色ID获取拥有该角色的所有用户ID列表
     *
     * @param roleId 角色ID
     * @return List<String> 用户ID列表，如果角色不存在或没有用户则返回空列表
     */
    fun getRoleUserIds(roleId: String): List<String>

    /**
     * 根据角色ID获取该角色拥有的所有资源ID列表
     *
     * @param roleId 角色ID
     * @return List<String> 资源ID列表，如果角色不存在或没有资源则返回空列表
     */
    fun getRoleResourceIds(roleId: String): List<String>

    /**
     * 根据租户ID获取该租户下所有激活角色的ID列表
     * 只返回active=true的角色ID
     *
     * @param tenantId 租户ID
     * @return List<String> 角色ID列表
     */
    fun getRoleIds(tenantId: String): List<String>

    /**
     * 根据角色ID获取拥有该角色的所有用户列表
     *
     * @param roleId 角色ID
     * @return List<AuthUserCacheItem> 用户列表，如果角色不存在或没有用户则返回空列表
     */
    fun getRoleUsers(roleId: String): List<AuthUserCacheItem>

    /**
     * 根据角色ID获取该角色拥有的所有资源列表
     *
     * @param roleId 角色ID
     * @return List<SysResourceCacheItem> 资源列表，如果角色不存在或没有资源则返回空列表
     */
    fun getRoleResources(roleId: String): List<SysResourceCacheItem>

    /**
     * 检查角色是否拥有指定资源
     *
     * @param roleId 角色ID
     * @param resourceId 资源ID
     * @return true表示角色拥有该资源，false表示不拥有
     */
    fun hasResource(roleId: String, resourceId: String): Boolean

    /**
     * 根据租户ID和角色编码获取角色信息
     *
     * @param tenantId 租户ID
     * @param roleCode 角色编码
     * @return 角色缓存项，找不到返回null
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getRoleByTenantIdAndCode(tenantId: String, roleCode: String): AuthRoleCacheItem?

    /**
     * 根据ID获取角色记录（非缓存）
     *
     * @param id 角色ID
     * @return 角色记录，找不到返回null
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getRoleRecord(id: String): AuthRoleRecord?

    /**
     * 根据租户ID获取角色列表
     *
     * @param tenantId 租户ID
     * @return 角色记录列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getRolesByTenantId(tenantId: String): List<AuthRoleRecord>

    /**
     * 根据子系统编码获取角色列表
     *
     * @param tenantId 租户ID
     * @param subsysCode 子系统编码
     * @return 角色记录列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getRolesBySubsysCode(tenantId: String, subsysCode: String): List<AuthRoleRecord>

    /**
     * 更新角色启用状态
     *
     * @param id 角色ID
     * @param active 是否启用
     * @return 是否更新成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun updateActive(id: String, active: Boolean): Boolean

    //endregion your codes 2

}
