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
 * Role-Resource relation business
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
    override fun getResourceIdsByRoleId(roleId: String): Set<String> =
        resourceIdsByRoleIdCache.getResourceIds(roleId).toSet()

    @Transactional(readOnly = true)
    override fun getRoleIdsByResourceId(resourceId: String): Set<String> =
        dao.searchRoleIdsByResourceId(resourceId)

    @Transactional
    override fun batchBind(roleId: String, resourceIds: Collection<String>): Int {
        if (resourceIds.isEmpty()) return 0
        // SELECT existing relations once (resource_id is character(N); DB may return strings with padding, trim uniformly).
        val existing = dao.searchResourceIdsByRoleIds(listOf(roleId)).mapTo(mutableSetOf()) { it.trim() }
        val boundResourceIds = resourceIds.mapTo(mutableSetOf()) { it.trim() } - existing
        if (boundResourceIds.isEmpty()) {
            log.debug("Batch binding relations between role ${roleId} and ${resourceIds.size} resources; all already exist, nothing added.")
            return 0
        }
        val relations = boundResourceIds.map { trimmed ->
            AuthRoleResource {
                this.roleId = roleId
                this.resourceId = trimmed
            }
        }
        dao.batchInsert(relations)
        log.debug("Batch binding relations between role ${roleId} and ${resourceIds.size} resources; successfully bound ${boundResourceIds.size} entries.")
        eventPublisher.publishEvent(AuthRoleResourceRelationsChanged(roleId, boundResourceIds.toList()))
        return boundResourceIds.size
    }

    @Transactional
    override fun unbind(roleId: String, resourceId: String): Boolean {
        val trimmed = resourceId.trim()
        val count = dao.deleteByRoleIdAndResourceId(roleId, trimmed)
        val success = count > 0
        if (success) {
            log.debug("Unbinding relation between role ${roleId} and resource ${resourceId}.")
            eventPublisher.publishEvent(AuthRoleResourceRelationsChanged(roleId, listOf(trimmed)))
        } else {
            log.warn("Failed to unbind relation between role ${roleId} and resource ${resourceId}; relation does not exist.")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun exists(roleId: String, resourceId: String): Boolean =
        dao.exists(roleId, resourceId.trim())


}
