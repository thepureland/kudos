package io.kudos.ams.auth.provider.service.impl

import io.kudos.ams.auth.common.vo.user.AuthUserCacheItem
import io.kudos.ams.auth.provider.service.iservice.IAuthRoleService
import io.kudos.ams.auth.provider.model.po.AuthRole
import io.kudos.ams.auth.provider.dao.AuthRoleDao
import io.kudos.ams.auth.provider.cache.UserByIdCacheHandler
import io.kudos.ams.auth.provider.cache.UserIdsByRoleIdCacheHandler
import io.kudos.ams.auth.provider.cache.ResourceIdsByRoleIdCacheHandler
import io.kudos.ams.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.ams.sys.provider.cache.ResourceByIdCacheHandler
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


/**
 * 角色业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class AuthRoleService : BaseCrudService<String, AuthRole, AuthRoleDao>(), IAuthRoleService {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var userIdsByRoleIdCacheHandler: UserIdsByRoleIdCacheHandler

    @Autowired
    private lateinit var resourceIdsByRoleIdCacheHandler: ResourceIdsByRoleIdCacheHandler

    @Autowired
    private lateinit var userByIdCacheHandler: UserByIdCacheHandler

    @Autowired
    private lateinit var resourceByIdCacheHandler: ResourceByIdCacheHandler

    override fun getRoleUserIds(roleId: String): List<String> {
        return userIdsByRoleIdCacheHandler.getUserIds(roleId)
    }

    override fun getRoleResourceIds(roleId: String): List<String> {
        return resourceIdsByRoleIdCacheHandler.getResourceIds(roleId)
    }

    override fun getRoleIds(tenantId: String): List<String> {
        val criteria = Criteria(AuthRole::tenantId.name, OperatorEnum.EQ, tenantId)
            .addAnd(AuthRole::active.name, OperatorEnum.EQ, true)
        @Suppress("UNCHECKED_CAST")
        val roles = search(criteria) as List<AuthRole>
        return roles.mapNotNull { it.id }
    }

    override fun getRoleUsers(roleId: String): List<AuthUserCacheItem> {
        val userIds = getRoleUserIds(roleId)
        if (userIds.isEmpty()) {
            return emptyList()
        }
        val usersMap = userByIdCacheHandler.getUsersByIds(userIds)
        return userIds.mapNotNull { usersMap[it] }
    }

    override fun getRoleResources(roleId: String): List<SysResourceCacheItem> {
        val resourceIds = getRoleResourceIds(roleId)
        if (resourceIds.isEmpty()) {
            return emptyList()
        }
        val resourcesMap = resourceByIdCacheHandler.getResourcesByIds(resourceIds)
        return resourceIds.mapNotNull { resourcesMap[it] }
    }

    override fun hasResource(roleId: String, resourceId: String): Boolean {
        val resourceIds = getRoleResourceIds(roleId)
        return resourceIds.contains(resourceId)
    }

    //endregion your codes 2

}
