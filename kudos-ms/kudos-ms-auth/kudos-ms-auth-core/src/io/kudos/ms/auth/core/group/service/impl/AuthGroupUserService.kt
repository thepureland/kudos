package io.kudos.ms.auth.core.group.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupUserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * Group-user relation service.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
@Transactional
open class AuthGroupUserService(
    dao: AuthGroupUserDao
) : BaseCrudService<String, AuthGroupUser, AuthGroupUserDao>(dao),
    IAuthGroupUserService {


    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getUserIdsByGroupId(groupId: String): Set<String> =
        dao.searchUserIdsByGroupId(groupId)

    @Transactional(readOnly = true)
    override fun getGroupIdsByUserId(userId: String): Set<String> =
        dao.searchGroupIdsByUserId(userId)

    @Transactional
    override fun batchBind(groupId: String, userIds: Collection<String>): Int {
        if (userIds.isEmpty()) return 0
        // One SELECT for existing relations, then a single batchInsert for the delta — collapses the original N+1 into 2 SQL calls.
        val existing = dao.searchUserIdsByGroupId(groupId)
        val boundUserIds = userIds.toSet() - existing
        if (boundUserIds.isEmpty()) {
            log.debug("Batch-binding group ${groupId} to ${userIds.size} users: all already exist, nothing inserted.")
            return 0
        }
        val relations = boundUserIds.map { userId ->
            AuthGroupUser {
                this.groupId = groupId
                this.userId = userId
            }
        }
        dao.batchInsert(relations)
        log.debug("Batch-bound group ${groupId} to ${userIds.size} users, ${boundUserIds.size} new bindings inserted.")
        eventPublisher.publishEvent(AuthGroupUserRelationsChanged(groupId, boundUserIds.toList()))
        return boundUserIds.size
    }

    @Transactional
    override fun unbind(groupId: String, userId: String): Boolean {
        val count = dao.deleteByGroupIdAndUserId(groupId, userId)
        val success = count > 0
        if (success) {
            log.debug("Unbound group ${groupId} from user ${userId}.")
            eventPublisher.publishEvent(AuthGroupUserRelationsChanged(groupId, listOf(userId)))
        } else {
            log.warn("Failed to unbind group ${groupId} from user ${userId}: relation does not exist.")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun exists(groupId: String, userId: String): Boolean = dao.exists(groupId, userId)


}
