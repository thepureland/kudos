package io.kudos.ams.auth.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.ams.auth.core.cache.ResourceIdsByRoleIdCacheHandler
import io.kudos.ams.auth.core.cache.ResourceIdsByUserIdCacheHandler
import io.kudos.ams.auth.core.dao.AuthRoleResourceDao
import io.kudos.ams.auth.core.dao.AuthRoleUserDao
import io.kudos.ams.auth.core.model.po.AuthRoleResource
import io.kudos.ams.auth.core.model.po.AuthRoleUser
import io.kudos.ams.auth.core.service.iservice.IAuthRoleResourceService
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 角色-资源关系业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class AuthRoleResourceService : BaseCrudService<String, AuthRoleResource, AuthRoleResourceDao>(),
    IAuthRoleResourceService {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var resourceIdsByRoleIdCacheHandler: ResourceIdsByRoleIdCacheHandler

    @Autowired
    private lateinit var resourceIdsByUserIdCacheHandler: ResourceIdsByUserIdCacheHandler

    @Autowired
    private lateinit var authRoleUserDao: AuthRoleUserDao

    private val log = LogFactory.getLog(this)

    override fun getResourceIdsByRoleId(roleId: String): Set<String> {
        return resourceIdsByRoleIdCacheHandler.getResourceIds(roleId).toSet()
    }

    override fun getRoleIdsByResourceId(resourceId: String): Set<String> {
        return dao.searchRoleIdsByResourceId(resourceId)
    }

    @Transactional
    override fun batchBind(roleId: String, resourceIds: Collection<String>): Int {
        if (resourceIds.isEmpty()) {
            return 0
        }
        var count = 0
        resourceIds.forEach { resourceId ->
            if (!exists(roleId, resourceId)) {
                val relation = AuthRoleResource.Companion {
                    this.roleId = roleId
                    this.resourceId = resourceId.trim()
                }
                dao.insert(relation)
                count++
            }
        }
        log.debug("批量绑定角色${roleId}与${resourceIds.size}个资源的关系，成功绑定${count}条。")
        // 同步缓存
        resourceIdsByRoleIdCacheHandler.syncOnRoleResourceChange(roleId)
        // 同步该角色下所有用户的资源缓存
        val roleUserCriteria = Criteria.of(AuthRoleUser::roleId.name, OperatorEnum.EQ, roleId)
        val roleUsers = authRoleUserDao.search(roleUserCriteria)
        roleUsers.map { it.userId }.distinct().forEach { _ ->
            resourceIdsByUserIdCacheHandler.syncOnRoleResourceChange(roleId)
        }
        return count
    }

    @Transactional
    override fun unbind(roleId: String, resourceId: String): Boolean {
        val criteria = Criteria.of(AuthRoleResource::roleId.name, OperatorEnum.EQ, roleId)
            .addAnd(AuthRoleResource::resourceId.name, OperatorEnum.EQ, resourceId.trim())
        val count = dao.batchDeleteCriteria(criteria)
        val success = count > 0
        if (success) {
            log.debug("解绑角色${roleId}与资源${resourceId}的关系。")
            // 同步缓存
            resourceIdsByRoleIdCacheHandler.syncOnRoleResourceChange(roleId)
            // 同步该角色下所有用户的资源缓存
            val roleUserCriteria = Criteria.of(AuthRoleUser::roleId.name, OperatorEnum.EQ, roleId)
            val roleUsers = authRoleUserDao.search(roleUserCriteria)
            roleUsers.map { it.userId }.distinct().forEach { _ ->
                resourceIdsByUserIdCacheHandler.syncOnRoleResourceChange(roleId)
            }
        } else {
            log.warn("解绑角色${roleId}与资源${resourceId}的关系失败，关系不存在。")
        }
        return success
    }

    override fun exists(roleId: String, resourceId: String): Boolean {
        return dao.exists(roleId, resourceId.trim())
    }

    //endregion your codes 2

}
