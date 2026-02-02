package io.kudos.ms.user.core.api

import io.kudos.ms.user.common.api.IUserAccountApi
import io.kudos.ms.user.common.vo.org.UserOrgCacheItem
import io.kudos.ms.user.common.vo.user.UserAccountCacheItem
import io.kudos.ms.user.core.cache.UserByIdCacheHandler
import io.kudos.ms.user.core.cache.UserIdByTenantIdAndUsernameCacheHandler
import io.kudos.ms.user.core.service.iservice.IUserAccountService
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
open class UserAccountApi : IUserAccountApi {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var userByIdCacheHandler: UserByIdCacheHandler

    @Autowired
    private lateinit var userIdByTenantIdAndUsernameCacheHandler: UserIdByTenantIdAndUsernameCacheHandler

    @Autowired
    private lateinit var userAccountService: IUserAccountService

    override fun getUserById(id: String): UserAccountCacheItem? {
        return userByIdCacheHandler.getUserById(id)
    }

    override fun getUsersByIds(ids: Collection<String>): Map<String, UserAccountCacheItem> {
        return userByIdCacheHandler.getUsersByIds(ids)
    }

    override fun getUserId(tenantId: String, username: String): String? {
        return userIdByTenantIdAndUsernameCacheHandler.getUserId(tenantId, username)
    }

    override fun isUserInOrg(userId: String, orgId: String): Boolean {
        return userAccountService.isUserInOrg(userId, orgId)
    }

    override fun getUserOrgs(userId: String): List<UserOrgCacheItem> {
        return userAccountService.getUserOrgs(userId)
    }

    override fun getUserIds(tenantId: String): List<String> {
        return userAccountService.getUserIds(tenantId)
    }

    //endregion your codes 2

}
