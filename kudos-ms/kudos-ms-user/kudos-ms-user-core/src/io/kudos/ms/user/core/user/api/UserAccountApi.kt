package io.kudos.ms.user.core.user.api
import io.kudos.ms.user.common.user.api.IUserAccountApi
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.common.user.vo.UserAccountCacheEntry
import io.kudos.ms.user.core.user.cache.UserAccountHashCache
import io.kudos.ms.user.core.user.service.iservice.IUserAccountService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


/**
 * 用户 API本地实现
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
open class UserAccountApi : IUserAccountApi {


    @Autowired
    private lateinit var userAccountHashCache: UserAccountHashCache

    @Autowired
    private lateinit var userAccountService: IUserAccountService

    override fun getUserById(id: String): UserAccountCacheEntry? {
        return userAccountHashCache.getUserById(id)
    }

    override fun getUsersByIds(ids: Collection<String>): Map<String, UserAccountCacheEntry> {
        return userAccountHashCache.getUsersByIds(ids)
    }

    override fun getUserId(tenantId: String, username: String): String? {
        return userAccountHashCache.getUsersByTenantIdAndUsername(tenantId, username)?.id
    }

    override fun isUserInOrg(userId: String, orgId: String): Boolean {
        return userAccountService.isUserInOrg(userId, orgId)
    }

    override fun getUserOrgs(userId: String): List<UserOrgCacheEntry> {
        return userAccountService.getUserOrgs(userId)
    }

    override fun getUserIds(tenantId: String): List<String> {
        return userAccountService.getUserIds(tenantId)
    }


}
