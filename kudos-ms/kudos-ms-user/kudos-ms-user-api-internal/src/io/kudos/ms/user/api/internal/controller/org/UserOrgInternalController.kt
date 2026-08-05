package io.kudos.ms.user.api.internal.controller.org

import io.kudos.ms.user.common.account.support.eraseCredentials
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.org.api.IUserOrgApi
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.core.org.api.UserOrgApi
import org.springframework.web.bind.annotation.RestController


/**
 * Organization internal RPC controller. Paths are inherited from method-level annotations on [IUserOrgApi].
 *
 * Account entries are credential-erased ([eraseCredentials]) before leaving this provider —
 * see [io.kudos.ms.user.api.internal.controller.account.UserAccountInternalController].
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class UserOrgInternalController(
    private val userOrgApi: UserOrgApi,
) : IUserOrgApi {

    override fun getOrgById(id: String): UserOrgCacheEntry? =
        userOrgApi.getOrgById(id)

    override fun getOrgsByIds(ids: Collection<String>): Map<String, UserOrgCacheEntry> =
        userOrgApi.getOrgsByIds(ids)

    override fun getOrgIds(tenantId: String): List<String> =
        userOrgApi.getOrgIds(tenantId)

    override fun getOrgAdmins(orgId: String): List<UserAccountCacheEntry> =
        userOrgApi.getOrgAdmins(orgId).map { it.eraseCredentials() }

    override fun getOrgUsers(orgId: String): List<UserAccountCacheEntry> =
        userOrgApi.getOrgUsers(orgId).map { it.eraseCredentials() }

    override fun isUserInOrg(userId: String, orgId: String): Boolean =
        userOrgApi.isUserInOrg(userId, orgId)

    override fun getChildOrgs(orgId: String): List<UserOrgCacheEntry> =
        userOrgApi.getChildOrgs(orgId)

    override fun getParentOrg(orgId: String): UserOrgCacheEntry? =
        userOrgApi.getParentOrg(orgId)

}
