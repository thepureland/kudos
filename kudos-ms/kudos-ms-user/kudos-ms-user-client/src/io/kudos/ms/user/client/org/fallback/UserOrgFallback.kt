package io.kudos.ms.user.client.org.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.user.client.org.proxy.IUserOrgProxy
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import org.springframework.stereotype.Component


/**
 * 机构 Feign 容错降级实现。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class UserOrgFallback :
    AbstractFeignFallbackSupport("UserOrgFallback"), IUserOrgProxy {

    override fun getOrgById(id: String): UserOrgCacheEntry? {
        warnRead("getOrgById", id)
        return null
    }

    override fun getOrgsByIds(ids: Collection<String>): Map<String, UserOrgCacheEntry> {
        warnRead("getOrgsByIds", ids)
        return emptyMap()
    }

    override fun getOrgIds(tenantId: String): List<String> {
        warnRead("getOrgIds", tenantId)
        return emptyList()
    }

    override fun getOrgAdmins(orgId: String): List<UserAccountCacheEntry> {
        warnRead("getOrgAdmins", orgId)
        return emptyList()
    }

    override fun getOrgUsers(orgId: String): List<UserAccountCacheEntry> {
        warnRead("getOrgUsers", orgId)
        return emptyList()
    }

    override fun isUserInOrg(userId: String, orgId: String): Boolean {
        warnRead("isUserInOrg", userId, orgId)
        return false
    }

    override fun getChildOrgs(orgId: String): List<UserOrgCacheEntry> {
        warnRead("getChildOrgs", orgId)
        return emptyList()
    }

    override fun getParentOrg(orgId: String): UserOrgCacheEntry? {
        warnRead("getParentOrg", orgId)
        return null
    }
}
