package io.kudos.ms.user.core.account.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.user.core.account.dao.UserOrgUserDao
import io.kudos.ms.user.core.account.event.UserOrgUserAdminUpdated
import io.kudos.ms.user.core.account.event.UserOrgUserRelationsChanged
import io.kudos.ms.user.core.account.model.po.UserOrgUser
import io.kudos.ms.user.core.account.service.iservice.IUserOrgUserService
import io.kudos.ms.user.core.org.cache.OrgIdsByUserIdCache
import io.kudos.ms.user.core.org.cache.UserIdsByOrgIdCache
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * Organization-user association service implementation.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class UserOrgUserService(
    dao: UserOrgUserDao
) : BaseCrudService<String, UserOrgUser, UserOrgUserDao>(dao), IUserOrgUserService {


    @Autowired
    private lateinit var userIdsByOrgIdCache: UserIdsByOrgIdCache

    @Autowired
    private lateinit var orgIdsByUserIdCache: OrgIdsByUserIdCache

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getUserIdsByOrgId(orgId: String): Set<String> =
        userIdsByOrgIdCache.getUserIds(orgId).toSet()

    @Transactional(readOnly = true)
    override fun getOrgIdsByUserId(userId: String): Set<String> =
        orgIdsByUserIdCache.getOrgIds(userId).toSet()

    @Transactional
    override fun batchBind(orgId: String, userIds: Collection<String>, orgAdmin: Boolean): Int {
        if (userIds.isEmpty()) return 0
        // One SELECT for existing associations, then one batchInsert for the new ids in the diff,
        // collapsing the original N+1 down to 2 SQL statements.
        val existing = dao.searchUserIdsByOrgId(orgId).toSet()
        val boundUserIds = userIds.toSet() - existing
        if (boundUserIds.isEmpty()) {
            log.debug("Batch binding organization ${orgId} with ${userIds.size} users; all already exist, no inserts.")
            return 0
        }
        val relations = boundUserIds.map { userId ->
            UserOrgUser {
                this.orgId = orgId
                this.userId = userId
                this.orgAdmin = orgAdmin
            }
        }
        dao.batchInsert(relations)
        log.debug("Batch bound organization ${orgId} with ${userIds.size} users; ${boundUserIds.size} new bindings created.")
        eventPublisher.publishEvent(UserOrgUserRelationsChanged(orgId, boundUserIds.toList()))
        return boundUserIds.size
    }

    @Transactional
    override fun unbind(orgId: String, userId: String): Boolean {
        val count = dao.deleteByOrgIdAndUserId(orgId, userId)
        val success = count > 0
        if (success) {
            log.debug("Unbound association between organization ${orgId} and user ${userId}.")
            eventPublisher.publishEvent(UserOrgUserRelationsChanged(orgId, listOf(userId)))
        } else {
            log.warn("Failed to unbind organization ${orgId} and user ${userId}: association does not exist.")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun exists(orgId: String, userId: String): Boolean = dao.exists(orgId, userId)

    @Transactional
    override fun setOrgAdmin(orgId: String, userId: String, isAdmin: Boolean): Boolean {
        val relation = dao.searchByOrgIdAndUserId(orgId, userId).firstOrNull() ?: run {
            log.warn("Failed to set user ${userId} as admin of organization ${orgId}: association does not exist.")
            return false
        }
        val updated = UserOrgUser {
            this.id = relation.id
            this.orgId = orgId
            this.userId = userId
            this.orgAdmin = isAdmin
        }
        val success = dao.update(updated)
        if (success) {
            log.debug("Set user ${userId} as admin of organization ${orgId}: ${isAdmin}.")
            // The cache does not include the orgAdmin field, but we still publish the event to provide
            // a downstream consistency extension point.
            eventPublisher.publishEvent(UserOrgUserAdminUpdated(relation.id, orgId))
        } else {
            log.error("Failed to set user ${userId} as admin of organization ${orgId}!")
        }
        return success
    }


}
