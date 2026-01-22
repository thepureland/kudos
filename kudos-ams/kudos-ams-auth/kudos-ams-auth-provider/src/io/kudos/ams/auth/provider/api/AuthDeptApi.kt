package io.kudos.ams.auth.provider.api

import io.kudos.ams.auth.common.api.IAuthDeptApi
import io.kudos.ams.auth.common.vo.dept.AuthDeptCacheItem
import io.kudos.ams.auth.common.vo.user.AuthUserCacheItem
import io.kudos.ams.auth.provider.cache.DeptByIdCacheHandler
import io.kudos.ams.auth.provider.cache.DeptIdsByTenantIdCacheHandler
import io.kudos.ams.auth.provider.service.iservice.IAuthDeptService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


/**
 * 部门 API本地实现
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
@Service
open class AuthDeptApi : IAuthDeptApi {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var deptByIdCacheHandler: DeptByIdCacheHandler

    @Autowired
    private lateinit var deptIdsByTenantIdCacheHandler: DeptIdsByTenantIdCacheHandler

    @Autowired
    private lateinit var authDeptService: IAuthDeptService

    override fun getDeptById(id: String): AuthDeptCacheItem? {
        return deptByIdCacheHandler.getDeptById(id)
    }

    override fun getDeptsByIds(ids: Collection<String>): Map<String, AuthDeptCacheItem> {
        return deptByIdCacheHandler.getDeptsByIds(ids)
    }

    override fun getDeptIds(tenantId: String): List<String> {
        return deptIdsByTenantIdCacheHandler.getDeptIds(tenantId)
    }

    override fun getDeptAdmins(deptId: String): List<AuthUserCacheItem> {
        return authDeptService.getDeptAdmins(deptId)
    }

    //endregion your codes 2

}
