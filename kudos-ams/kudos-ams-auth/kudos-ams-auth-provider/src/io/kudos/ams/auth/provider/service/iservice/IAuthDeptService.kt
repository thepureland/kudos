package io.kudos.ams.auth.provider.service.iservice

import io.kudos.ams.auth.common.vo.user.AuthUserCacheItem
import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ams.auth.provider.model.po.AuthDept


/**
 * 部门业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface IAuthDeptService : IBaseCrudService<String, AuthDept> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据部门ID获取该部门的所有管理员用户信息
     *
     * @param deptId 部门ID
     * @return List<AuthUserCacheItem> 部门管理员用户列表，如果没有管理员则返回空列表
     */
    fun getDeptAdmins(deptId: String): List<AuthUserCacheItem>

    //endregion your codes 2

}
