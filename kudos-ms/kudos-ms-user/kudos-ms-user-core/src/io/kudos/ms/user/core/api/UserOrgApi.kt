package io.kudos.ms.user.core.api

import io.kudos.ms.user.common.api.IUserOrgApi
import io.kudos.ms.user.common.vo.org.UserOrgCacheEntry
import io.kudos.ms.user.common.vo.user.UserAccountCacheEntry
import io.kudos.ms.user.core.cache.UserOrgHashCache
import io.kudos.ms.user.core.service.iservice.IUserOrgService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service


/**
 * 机构 API本地实现
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
@Service
open class UserOrgApi : IUserOrgApi {
//endregion your codes 1

    //region your codes 2

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

    //endregion your codes 2

}
