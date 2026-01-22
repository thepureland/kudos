package io.kudos.ams.auth.provider.api

import io.kudos.ams.auth.common.api.IAuthRoleApi
import io.kudos.ams.auth.common.vo.role.AuthRoleCacheItem
import io.kudos.ams.auth.provider.cache.RoleByIdCacheHandler
import io.kudos.ams.auth.provider.cache.RoleIdByTenantIdAndRoleCodeCacheHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


/**
 * 角色 API本地实现
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
@Service
open class AuthRoleApi : IAuthRoleApi {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var roleByIdCacheHandler: RoleByIdCacheHandler

    @Autowired
    private lateinit var roleIdByTenantIdAndRoleCodeCacheHandler: RoleIdByTenantIdAndRoleCodeCacheHandler

    override fun getRoleById(id: String): AuthRoleCacheItem? {
        return roleByIdCacheHandler.getRoleById(id)
    }

    override fun getRolesByIds(ids: Collection<String>): Map<String, AuthRoleCacheItem> {
        return roleByIdCacheHandler.getRolesByIds(ids)
    }

    override fun getRoleId(tenantId: String, code: String): String? {
        return roleIdByTenantIdAndRoleCodeCacheHandler.getRoleId(tenantId, code)
    }

    //endregion your codes 2

}
