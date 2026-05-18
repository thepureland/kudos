package io.kudos.ms.user.api.internal.controller.account

import io.kudos.ms.user.common.account.api.IUserAccountApi
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.core.account.api.UserAccountApi
import org.springframework.web.bind.annotation.RestController


/**
 * 用户账号 内部 RPC 控制器。路径继承自 [IUserAccountApi] 方法级注解。
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class UserAccountInternalController(
    private val userAccountApi: UserAccountApi,
) : IUserAccountApi {

    override fun getUserById(id: String): UserAccountCacheEntry? =
        userAccountApi.getUserById(id)

    override fun getUsersByIds(ids: Collection<String>): Map<String, UserAccountCacheEntry> =
        userAccountApi.getUsersByIds(ids)

    override fun getUserId(tenantId: String, username: String): String? =
        userAccountApi.getUserId(tenantId, username)

    override fun getUserOrgs(userId: String): List<UserOrgCacheEntry> =
        userAccountApi.getUserOrgs(userId)

    override fun isUserInOrg(userId: String, orgId: String): Boolean =
        userAccountApi.isUserInOrg(userId, orgId)

    override fun getUserIds(tenantId: String): List<String> =
        userAccountApi.getUserIds(tenantId)

}
