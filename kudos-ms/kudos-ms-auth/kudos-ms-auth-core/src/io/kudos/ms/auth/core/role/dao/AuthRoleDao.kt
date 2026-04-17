package io.kudos.ms.auth.core.role.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.auth.core.role.model.table.AuthRoles
import org.springframework.stereotype.Repository


/**
 * 角色数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class AuthRoleDao : BaseCrudDao<String, AuthRole, AuthRoles>() {



    /**
     * 查询所有 active=true 的角色
     *
     * @return List<AuthRoleCacheEntry>
     */
    open fun searchActiveRolesForCache(): List<AuthRoleCacheEntry> {
        val criteria = Criteria(AuthRole::active eq true)
        return searchAs<AuthRoleCacheEntry>(criteria)
    }

    /**
     * 按租户、角色编码查询（不区分 active），返回单条缓存用 VO
     *
     * @param tenantId 租户id
     * @param code 角色编码
     * @return AuthRoleCacheEntry，不存在返回null
     */
    open fun searchRoleByTenantIdAndRoleCode(tenantId: String, code: String): AuthRoleCacheEntry? {
        val criteria = Criteria.and(
            AuthRole::tenantId eq tenantId,
            AuthRole::code eq code
        )
        return searchAs<AuthRoleCacheEntry>(criteria).firstOrNull()
    }

    /**
     * 查询租户下所有启用角色ID
     *
     * @param tenantId 租户ID
     * @return 角色ID列表
     */
    fun searchActiveRoleIdsByTenantId(tenantId: String): List<String> {
        val criteria = Criteria(AuthRole::tenantId eq tenantId)
            .addAnd(AuthRole::active eq true)
        return searchProperty(criteria, AuthRole::id).filterNotNull()
    }


}
