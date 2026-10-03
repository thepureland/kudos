package io.kudos.ms.user.core.account.service.impl

import io.kudos.base.bean.BeanKit
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.org.dao.UserOrgDao
import io.kudos.ms.user.core.org.service.impl.OrganizationDirectoryGate
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

    /** Organization-mode checks; absent only in pure unit tests, where only legacy rows exist. */
    @Autowired(required = false)
    private var directoryGate: OrganizationDirectoryGate? = null

    @Autowired(required = false)
    private var userOrgDao: UserOrgDao? = null

    @Autowired(required = false)
    private var userAccountDao: UserAccountDao? = null

    private val log = LogFactory.getLog(this::class)

    /**
     * Membership in an organization's department is a directory write of that organization, and the
     * member must belong to it. Legacy departments keep their previous (unchecked) behaviour.
     */
    private fun assertOrganizationRelation(orgId: String, userIds: Collection<String>) {
        val department = userOrgDao?.get(orgId) ?: return
        val organizationId = department.organizationId ?: return
        requireNotNull(directoryGate) { "ORGANIZATION_MODE_UNAVAILABLE" }.assertCanManageDirectory(organizationId)
        val accounts = requireNotNull(userAccountDao).getByIds(userIds).associateBy { it.id }
        userIds.forEach { userId ->
            require(accounts[userId]?.organizationId == organizationId) { "Department and member must belong to the same organization" }
        }
    }

    @Transactional(readOnly = true)
    override fun getUserIdsByOrgId(orgId: String): Set<String> =
        userIdsByOrgIdCache.getUserIds(orgId).toSet()

    @Transactional(readOnly = true)
    override fun getOrgIdsByUserId(userId: String): Set<String> =
        orgIdsByUserIdCache.getOrgIds(userId).toSet()

    @Transactional
    override fun insert(any: Any): String {
        relationOf(any)?.let { (orgId, userId) -> assertOrganizationRelation(orgId, listOf(userId)) }
        return super.insert(any)
    }

    @Transactional
    override fun update(any: Any): Boolean {
        relationOf(any)?.let { (orgId, userId) -> assertOrganizationRelation(orgId, listOf(userId)) }
        return super.update(any)
    }

    private fun relationOf(any: Any): Pair<String, String>? {
        val orgId = runCatching { BeanKit.getProperty(any, UserOrgUser::orgId.name) as? String }.getOrNull()
        val userId = runCatching { BeanKit.getProperty(any, UserOrgUser::userId.name) as? String }.getOrNull()
        return if (orgId.isNullOrBlank() || userId.isNullOrBlank()) null else orgId to userId
    }

    @Transactional
    override fun batchBind(orgId: String, userIds: Collection<String>, orgAdmin: Boolean): Int {
        if (userIds.isEmpty()) return 0
        assertOrganizationRelation(orgId, userIds)
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
        userOrgDao?.get(orgId)?.organizationId?.let {
            requireNotNull(directoryGate) { "ORGANIZATION_MODE_UNAVAILABLE" }.assertCanManageDirectory(it)
        }
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
        assertOrganizationRelation(orgId, listOf(userId))
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
