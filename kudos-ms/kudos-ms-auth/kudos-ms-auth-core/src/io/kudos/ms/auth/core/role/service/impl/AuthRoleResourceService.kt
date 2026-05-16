package io.kudos.ms.auth.core.role.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.platform.cache.ResourceIdsByRoleIdCache
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.event.AuthRoleResourceRelationsChanged
import io.kudos.ms.auth.core.role.model.po.AuthRoleResource
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleResourceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
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
@Transactional
open class AuthRoleResourceService(
    dao: AuthRoleResourceDao
) : BaseCrudService<String, AuthRoleResource, AuthRoleResourceDao>(dao),
    IAuthRoleResourceService {


    @Autowired
    private lateinit var resourceIdsByRoleIdCache: ResourceIdsByRoleIdCache

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getResourceIdsByRoleId(roleId: String): Set<String> {
        return resourceIdsByRoleIdCache.getResourceIds(roleId).toSet()
    }

    @Transactional(readOnly = true)
    override fun getRoleIdsByResourceId(resourceId: String): Set<String> {
        return dao.searchRoleIdsByResourceId(resourceId)
    }

    @Transactional
    override fun batchBind(roleId: String, resourceIds: Collection<String>): Int {
        if (resourceIds.isEmpty()) {
            return 0
        }
        var count = 0
        val boundResourceIds = mutableListOf<String>()
        resourceIds.forEach { resourceId ->
            val trimmed = resourceId.trim()
            if (!exists(roleId, trimmed)) {
                val relation = AuthRoleResource.Companion {
                    this.roleId = roleId
                    this.resourceId = trimmed
                }
                dao.insert(relation)
                boundResourceIds += trimmed
                count++
            }
        }
        log.debug("批量绑定角色${roleId}与${resourceIds.size}个资源的关系，成功绑定${count}条。")
        if (boundResourceIds.isNotEmpty()) {
            eventPublisher.publishEvent(AuthRoleResourceRelationsChanged(roleId, boundResourceIds))
        }
        return count
    }

    @Transactional
    override fun unbind(roleId: String, resourceId: String): Boolean {
        val trimmed = resourceId.trim()
        val count = dao.deleteByRoleIdAndResourceId(roleId, trimmed)
        val success = count > 0
        if (success) {
            log.debug("解绑角色${roleId}与资源${resourceId}的关系。")
            eventPublisher.publishEvent(AuthRoleResourceRelationsChanged(roleId, listOf(trimmed)))
        } else {
            log.warn("解绑角色${roleId}与资源${resourceId}的关系失败，关系不存在。")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun exists(roleId: String, resourceId: String): Boolean {
        return dao.exists(roleId, resourceId.trim())
    }


}
