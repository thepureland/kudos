package io.kudos.ms.user.client.account.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.user.client.account.proxy.IUserAccountProxy
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import org.springframework.stereotype.Component


/**
 * User account Feign fallback.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class UserAccountFallback :
    AbstractFeignFallbackSupport("UserAccountFallback"), IUserAccountProxy {

    override fun getUserById(id: String): UserAccountCacheEntry? {
        warnRead("getUserById", id)
        return null
    }

    override fun getUsersByIds(ids: Collection<String>): Map<String, UserAccountCacheEntry> {
        warnRead("getUsersByIds", ids)
        return emptyMap()
    }

    override fun getUserId(tenantId: String, username: String): String? {
        warnRead("getUserId", tenantId, username)
        return null
    }

    override fun getUserOrgs(userId: String): List<UserOrgCacheEntry> {
        warnRead("getUserOrgs", userId)
        return emptyList()
    }

    override fun isUserInOrg(userId: String, orgId: String): Boolean {
        // Safe default: when the remote is unreachable, treat as "not a member" to avoid incorrect authorization.
        warnRead("isUserInOrg", userId, orgId)
        return false
    }

    override fun getUserIds(tenantId: String): List<String> {
        warnRead("getUserIds", tenantId)
        return emptyList()
    }
}
