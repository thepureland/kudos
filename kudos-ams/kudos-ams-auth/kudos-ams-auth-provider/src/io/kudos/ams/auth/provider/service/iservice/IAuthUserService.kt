package io.kudos.ams.auth.provider.service.iservice

import io.kudos.ams.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ams.auth.provider.model.po.AuthUser


/**
 * 用户业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface IAuthUserService : IBaseCrudService<String, AuthUser> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据用户ID获取该用户有权限访问的资源缓存对象列表
     * 查询流程：用户 → 角色 → 资源（三级关联）
     *
     * @param userId 用户ID
     * @return List<SysResourceCacheItem> 资源缓存对象列表，如果用户不存在或没有资源则返回空列表
     */
    fun getResources(userId: String): List<SysResourceCacheItem>

    //endregion your codes 2

}
