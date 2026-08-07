package io.kudos.ms.auth.core.group.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.group.vo.response.GroupMembershipVo
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupUserService
import io.kudos.ms.auth.core.policy.GrantCandidate
import io.kudos.ms.auth.core.policy.TenantAdministrationGuard
import io.kudos.ms.auth.core.policy.iservice.IAuthGrantPolicyService
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.auth.core.principal.spi.UserAccountPrincipalDirectory.Companion.PRINCIPAL_TYPE_USER
import io.kudos.ms.user.common.passport.CurrentUserKit
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime


/**
 * Group-user relation service.
 *
 * Membership is a grant path, not a directory feature: putting a principal into a group hands them
 * every role the group carries. [batchBind] therefore screens through [IAuthGrantPolicyService]
 * exactly as a direct role grant does — otherwise the group is simply the way to walk around the
 * constraints the direct path enforces.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
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

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Resource
    private lateinit var grantPolicyService: IAuthGrantPolicyService

    @Resource
    private lateinit var userAccountHashCache: UserAccountHashCache

    @Resource
    private lateinit var authGroupDao: AuthGroupDao

    @Resource
    private lateinit var tenantAdministrationGuard: TenantAdministrationGuard

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getUserIdsByGroupId(groupId: String): Set<String> {
        val group = requireGroup(groupId)
        tenantAdministrationGuard.assertCanManage(group.tenantId)
        return dao.searchUserIdsByGroupId(groupId)
    }

    @Transactional(readOnly = true)
    override fun getGroupIdsByUserId(userId: String): Set<String> =
        dao.searchGroupIdsByUserId(userId)

    @Transactional
    override fun batchBind(groupId: String, userIds: Collection<String>): Int {
        if (userIds.isEmpty()) return 0
        val group = requireGroup(groupId)
        tenantAdministrationGuard.assertCanManage(group.tenantId)
        userIds.toSet().forEach { userId ->
            tenantAdministrationGuard.assertPrincipalBelongsToTenant(userId, PRINCIPAL_TYPE_USER, group.tenantId)
        }
        // One SELECT for every row, because the (group, user) pair is unique: a member who was
        // removed still has a row, and it has to be *revived* rather than duplicated. Treating a
        // revoked row as "already a member" — which is what a bare id roster does — would silently
        // make re-adding somebody a no-op, the worst possible answer to "why can't they get back in".
        val rows = dao.searchMembershipsByGroupId(groupId)
        val requested = userIds.toSet()
        val liveMembers = rows.filter { it.revoked != true }.map { it.userId }.toSet()
        val revivable = rows.filter { it.revoked == true && it.userId in requested }
        val brandNew = requested - rows.map { it.userId }.toSet()
        val boundUserIds = brandNew + revivable.map { it.userId }.toSet()
        if (boundUserIds.isEmpty()) {
            log.debug("Batch-binding group ${groupId} to ${userIds.size} users: all already members, nothing to do.")
            return 0
        }

        // Joining a group *is* being granted every role the group carries, so this write is screened
        // exactly like a direct grant of each of those roles. Membership is one row per user and
        // cannot be applied partially, so a user rejected on any of the roles aborts the call —
        // with the offending (user, role) pairs named in the message.
        val groupRoleIds = authGroupRoleDao.searchRoleIdsByGroupId(groupId)
        if (groupRoleIds.isNotEmpty()) {
            grantPolicyService.assertNoRejection(
                grantPolicyService.screenGrants(
                    boundUserIds.flatMap { userId ->
                        groupRoleIds.map { roleId ->
                            GrantCandidate(roleId, userId, via = GrantCandidate.Via.GROUP_MEMBERSHIP)
                        }
                    },
                ),
            )
        }

        val operatorId = CurrentUserKit.currentUserIdOrNull()
        if (brandNew.isNotEmpty()) {
            dao.batchInsert(
                brandNew.map { userId ->
                    AuthGroupUser {
                        this.groupId = groupId
                        this.userId = userId
                        this.grantedBy = operatorId
                        this.revoked = false
                    }
                },
            )
        }
        // Reinstated in place: the row is the audit record of the whole relationship, and replacing
        // it would throw away when they first joined and why they were removed.
        revivable.forEach { row ->
            dao.update(AuthGroupUser {
                this.id = row.id
                this.grantedBy = operatorId
                this.revoked = false
                this.revokeReason = null
            })
        }
        log.debug(
            "Batch-bound group ${groupId}: ${brandNew.size} new, ${revivable.size} reinstated, " +
                "${liveMembers.size} already live.",
        )
        eventPublisher.publishEvent(AuthGroupUserRelationsChanged(groupId, boundUserIds.toList()))
        return boundUserIds.size
    }

    @Transactional
    override fun unbind(groupId: String, userId: String, reason: String?): Boolean {
        val group = requireGroup(groupId)
        tenantAdministrationGuard.assertCanManage(group.tenantId)
        // Soft, like every other revocation in this module. The `revoked` column had a read-path
        // filter and no writer — meaning removal was a hard delete and the audit trail for "who was
        // in this group, and who took them out" simply did not survive the act. Keeping the row is
        // also what lets a later re-add reinstate the original membership rather than mint a new one.
        val row = dao.searchMembershipsByGroupId(groupId).firstOrNull { it.userId == userId }
        if (row == null || row.revoked == true) {
            log.warn("Failed to unbind group ${groupId} from user ${userId}: no live membership.")
            return false
        }
        dao.update(AuthGroupUser {
            this.id = row.id
            this.revoked = true
            this.revokeReason = reason
        })
        log.debug("Revoked membership of ${userId} in group ${groupId}. Reason: ${reason ?: "none given"}.")
        eventPublisher.publishEvent(AuthGroupUserRelationsChanged(groupId, listOf(userId)))
        return true
    }

    @Transactional(readOnly = true)
    override fun exists(groupId: String, userId: String): Boolean {
        val group = authGroupDao.get(groupId) ?: return false
        tenantAdministrationGuard.assertCanManage(group.tenantId)
        return dao.exists(groupId, userId)
    }

    @Transactional(readOnly = true)
    override fun listMemberships(groupId: String): List<GroupMembershipVo> {
        val group = requireGroup(groupId)
        tenantAdministrationGuard.assertCanManage(group.tenantId)
        val rows = dao.searchMembershipsByGroupId(groupId).filter { it.revoked != true }
        if (rows.isEmpty()) return emptyList()
        val names = userAccountHashCache.getUsersByIds(rows.map { it.userId }.toSet())
        val now = LocalDateTime.now()
        return rows
            .map { row ->
                GroupMembershipVo(
                    id = row.id,
                    groupId = row.groupId,
                    userId = row.userId,
                    userName = names[row.userId]?.username,
                    startTime = row.startTime,
                    endTime = row.endTime,
                    active = isActiveAt(row.startTime, row.endTime, now),
                )
            }
            .sortedWith(compareBy({ !it.active }, { it.userName ?: it.userId }))
    }

    @Transactional
    override fun bindTemporal(
        groupId: String,
        userId: String,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
    ): String {
        val group = requireGroup(groupId)
        tenantAdministrationGuard.assertCanManage(group.tenantId)
        tenantAdministrationGuard.assertPrincipalBelongsToTenant(userId, PRINCIPAL_TYPE_USER, group.tenantId)
        if (startTime != null && endTime != null) {
            require(!startTime.isAfter(endTime)) {
                "start_time must not be after end_time (group=${groupId}, user=${userId})."
            }
        }
        // Screened as a grant of every role the group carries — a windowed membership is still a
        // membership, and skipping the check here would make "add them temporarily" the loophole.
        val groupRoleIds = authGroupRoleDao.searchRoleIdsByGroupId(groupId)
        if (groupRoleIds.isNotEmpty()) {
            grantPolicyService.assertNoRejection(
                grantPolicyService.screenGrants(
                    groupRoleIds.map { roleId ->
                        GrantCandidate(roleId, userId, via = GrantCandidate.Via.GROUP_MEMBERSHIP, endTime = endTime)
                    },
                ),
            )
        }

        // Replace semantics, so the supplied window is authoritative rather than one of two.
        dao.deleteByGroupIdAndUserId(groupId, userId)
        val id = dao.insert(
            AuthGroupUser {
                this.groupId = groupId
                this.userId = userId
                this.startTime = startTime
                this.endTime = endTime
                this.grantedBy = CurrentUserKit.currentUserIdOrNull()
                this.revoked = false
            },
        )
        log.debug("Group ${groupId} membership for ${userId} set to window [${startTime}, ${endTime}].")
        eventPublisher.publishEvent(AuthGroupUserRelationsChanged(groupId, listOf(userId)))
        return id
    }

    @Transactional
    override fun announceWindowChanges(since: LocalDateTime, now: LocalDateTime): Int {
        if (!since.isBefore(now)) return 0
        val crossed = dao.searchMembershipsCrossingWindowBoundary(since, now)
        if (crossed.isEmpty()) return 0
        // No row changes — only the cached snapshots are stale. One event per group so a group
        // whose members all lapsed at once costs a single eviction batch.
        crossed.groupBy { it.groupId }.forEach { (groupId, rows) ->
            eventPublisher.publishEvent(
                AuthGroupUserRelationsChanged(groupId, rows.map { it.userId }.distinct()),
            )
        }
        log.debug("Announced ${crossed.size} group membership window boundary crossing(s) in (${since}, ${now}].")
        return crossed.size
    }

    /** A membership is in force when now is within [start, end]; NULL bounds are open. */
    private fun isActiveAt(start: LocalDateTime?, end: LocalDateTime?, now: LocalDateTime): Boolean =
        (start == null || !start.isAfter(now)) && (end == null || !end.isBefore(now))

    private fun requireGroup(groupId: String) =
        authGroupDao.get(groupId) ?: throw IllegalArgumentException("Group not found: $groupId")

}
