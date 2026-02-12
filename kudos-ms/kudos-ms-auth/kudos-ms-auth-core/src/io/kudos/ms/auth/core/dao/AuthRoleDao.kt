package io.kudos.ms.auth.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.common.vo.role.AuthRoleCacheItem
import io.kudos.ms.auth.core.model.po.AuthRole
import io.kudos.ms.auth.core.model.table.AuthRoles
import org.springframework.stereotype.Repository


/**
 * 角色数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class AuthRoleDao : BaseCrudDao<String, AuthRole, AuthRoles>() {
//endregion your codes 1

    //region your codes 2


    /**
     * 查询所有 active=true 的角色
     *
     * @return List<AuthRoleCacheItem>
     */
    open fun searchActiveRolesForCache(): List<AuthRoleCacheItem> {
        val criteria = Criteria(AuthRole::active eq true)
        return searchAs<AuthRoleCacheItem>(criteria)
    }

    /**
     * 按租户、角色编码查询（不区分 active），返回单条缓存用 VO
     *
     * @param tenantId 租户id
     * @param code 角色编码
     * @return AuthRoleCacheItem，不存在返回null
     */
    open fun searchRoleByTenantIdAndRoleCode(tenantId: String, code: String): AuthRoleCacheItem? {
        val criteria = Criteria.and(
            AuthRole::tenantId eq tenantId,
            AuthRole::code eq code
        )
        return searchAs<AuthRoleCacheItem>(criteria).firstOrNull()
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

    //endregion your codes 2

}
