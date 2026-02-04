package io.kudos.ms.user.core.api

import io.kudos.ms.user.common.api.IUserAccountApi
import io.kudos.ms.user.common.vo.org.UserOrgCacheItem
import io.kudos.ms.user.common.vo.user.UserAccountCacheItem
import io.kudos.ms.user.core.cache.UserByIdCache
import io.kudos.ms.user.core.cache.UserIdByTenantIdAndUsernameCache
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
    private lateinit var userByIdCache: UserByIdCache

    @Autowired
    private lateinit var userIdByTenantIdAndUsernameCache: UserIdByTenantIdAndUsernameCache

    @Autowired
    private lateinit var userAccountService: IUserAccountService

    override fun getUserById(id: String): UserAccountCacheItem? {
        return userByIdCache.getUserById(id)
    }

    override fun getUsersByIds(ids: Collection<String>): Map<String, UserAccountCacheItem> {
        return userByIdCache.getUsersByIds(ids)
    }

    override fun getUserId(tenantId: String, username: String): String? {
        return userIdByTenantIdAndUsernameCache.getUserId(tenantId, username)
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
