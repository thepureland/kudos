package io.kudos.ms.auth.core.role.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleUserService
import io.kudos.ms.auth.core.platform.cache.ResourceIdsByUserIdCache
import io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache
import io.kudos.ms.auth.core.role.cache.UserIdsByRoleIdCache
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * Role-User relation business
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class AuthRoleUserService(
    dao: AuthRoleUserDao
) : BaseCrudService<String, AuthRoleUser, AuthRoleUserDao>(dao),
    IAuthRoleUserService {


    @Autowired
    private lateinit var userIdsByRoleIdCache: UserIdsByRoleIdCache

    @Autowired
    private lateinit var roleIdsByUserIdCache: RoleIdsByUserIdCache

    @Autowired
    private lateinit var resourceIdsByUserIdCache: ResourceIdsByUserIdCache

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getUserIdsByRoleId(roleId: String): Set<String> =
        userIdsByRoleIdCache.getUserIds(roleId).toSet()

    @Transactional(readOnly = true)
    override fun getRoleIdsByUserId(userId: String): Set<String> =
        roleIdsByUserIdCache.getRoleIds(userId).toSet()

    @Transactional
    override fun batchBind(roleId: String, userIds: Collection<String>): Int {
        if (userIds.isEmpty()) return 0
        // SELECT existing relations once, then batchInsert the delta of new IDs once, folding the original N+1 into 2 SQL calls.
        val existing = dao.searchUserIdsByRoleId(roleId).toSet()
        val boundUserIds = userIds.toSet() - existing
        if (boundUserIds.isEmpty()) {
            log.debug("Batch binding relations between role ${roleId} and ${userIds.size} users; all already exist, nothing added.")
            return 0
        }
        val relations = boundUserIds.map { userId ->
            AuthRoleUser {
                this.roleId = roleId
                this.userId = userId
            }
        }
        dao.batchInsert(relations)
        log.debug("Batch binding relations between role ${roleId} and ${userIds.size} users; successfully bound ${boundUserIds.size} entries.")
        eventPublisher.publishEvent(AuthRoleUserRelationsChanged(roleId, boundUserIds.toList()))
        return boundUserIds.size
    }

    @Transactional
    override fun unbind(roleId: String, userId: String): Boolean {
        val count = dao.deleteByRoleIdAndUserId(roleId, userId)
        val success = count > 0
        if (success) {
            log.debug("Unbinding relation between role ${roleId} and user ${userId}.")
            eventPublisher.publishEvent(AuthRoleUserRelationsChanged(roleId, listOf(userId)))
        } else {
            log.warn("Failed to unbind relation between role ${roleId} and user ${userId}; relation does not exist.")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun exists(roleId: String, userId: String): Boolean = dao.exists(roleId, userId)


}
