package io.kudos.ams.user.provider.api

import io.kudos.ams.user.common.api.IAuthUserApi
import io.kudos.ams.user.common.vo.dept.AuthDeptCacheItem
import io.kudos.ams.user.common.vo.user.AuthUserCacheItem
import io.kudos.ams.user.provider.cache.UserByIdCacheHandler
import io.kudos.ams.user.provider.cache.UserIdByTenantIdAndUsernameCacheHandler
import io.kudos.ams.user.provider.service.iservice.IAuthUserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


/**
 * 用户 API本地实现
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
@Service
open class AuthUserApi : IAuthUserApi {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var userByIdCacheHandler: UserByIdCacheHandler

    @Autowired
    private lateinit var userIdByTenantIdAndUsernameCacheHandler: UserIdByTenantIdAndUsernameCacheHandler

    @Autowired
    private lateinit var authUserService: IAuthUserService

    override fun getUserById(id: String): AuthUserCacheItem? {
        return userByIdCacheHandler.getUserById(id)
    }

    override fun getUsersByIds(ids: Collection<String>): Map<String, AuthUserCacheItem> {
        return userByIdCacheHandler.getUsersByIds(ids)
    }

    override fun getUserId(tenantId: String, username: String): String? {
        return userIdByTenantIdAndUsernameCacheHandler.getUserId(tenantId, username)
    }

    override fun isUserInDept(userId: String, deptId: String): Boolean {
        return authUserService.isUserInDept(userId, deptId)
    }

    override fun getUserDepts(userId: String): List<AuthDeptCacheItem> {
        return authUserService.getUserDepts(userId)
    }

    override fun getUserIds(tenantId: String): List<String> {
        return authUserService.getUserIds(tenantId)
    }

    //endregion your codes 2

}
