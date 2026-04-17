package io.kudos.ms.user.core.org.api

import io.kudos.ms.user.common.org.api.IUserOrgApi
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.core.org.cache.UserOrgHashCache
import io.kudos.ms.user.core.org.service.iservice.IUserOrgService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service


/**
 * 机构 API本地实现
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
open class UserOrgApi : IUserOrgApi {


    @Resource
    private lateinit var userOrgHashCache: UserOrgHashCache 

    @Resource
    private lateinit var userOrgService: IUserOrgService

    override fun getOrgById(id: String): UserOrgCacheEntry? {
        return userOrgHashCache.getOrgById(id)
    }

    override fun getOrgsByIds(ids: Collection<String>): Map<String, UserOrgCacheEntry> {
        return userOrgHashCache.getOrgsByIds(ids)
    }

    override fun getOrgIds(tenantId: String): List<String> {
        return userOrgHashCache.getOrgsByTenantId(tenantId).map { it.id }
    }

    override fun getOrgAdmins(orgId: String): List<UserAccountCacheEntry> {
        return userOrgService.getOrgAdmins(orgId)
    }

    override fun getOrgUsers(orgId: String): List<UserAccountCacheEntry> {
        return userOrgService.getOrgUsers(orgId)
    }

    override fun isUserInOrg(userId: String, orgId: String): Boolean {
        return userOrgService.isUserInOrg(userId, orgId)
    }

    override fun getChildOrgs(orgId: String): List<UserOrgCacheEntry> {
        return userOrgService.getChildOrgs(orgId)
    }

    override fun getParentOrg(orgId: String): UserOrgCacheEntry? {
        return userOrgService.getParentOrg(orgId)
    }


}
