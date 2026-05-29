package io.kudos.ms.auth.core.role.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.auth.common.role.vo.request.AuthRoleQuery
import io.kudos.ms.auth.common.role.vo.response.AuthRoleRow
import io.kudos.ms.auth.common.role.vo.response.BatchBindResultVo
import io.kudos.ms.auth.common.role.vo.response.EffectivePermissionsVo
import io.kudos.ms.auth.common.role.vo.response.RoleDeleteImpactVo
import io.kudos.ms.auth.core.group.cache.AuthGroupHashCache
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupRoleService
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupUserService
import io.kudos.ms.auth.core.platform.cache.ResourceIdsByRoleIdCache
import io.kudos.ms.auth.core.platform.cache.ResourceIdsByUserIdCache
import io.kudos.ms.auth.core.role.cache.AuthRoleHashCache
import io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache
import io.kudos.ms.auth.core.role.cache.UserIdsByRoleIdCache
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleBatchDeleted
import io.kudos.ms.auth.core.role.event.AuthRoleDeleted
import io.kudos.ms.auth.core.role.event.AuthRoleInserted
import io.kudos.ms.auth.core.role.event.AuthRoleUpdated
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleResourceService
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleService
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleUserService
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.sys.core.resource.cache.SysResourceHashCache
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import jakarta.annotation.Resource
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional


/**
 * Role business
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class AuthRoleService(
    dao: AuthRoleDao
) : BaseCrudService<String, AuthRole, AuthRoleDao>(dao),
    IAuthRoleService {


    @Resource
    private lateinit var userIdsByRoleIdCache: UserIdsByRoleIdCache

    @Resource
    private lateinit var resourceIdsByRoleIdCache: ResourceIdsByRoleIdCache

    @Resource
    private lateinit var userAccountHashCache: UserAccountHashCache

    @Resource
    private lateinit var sysResourceHashCache: SysResourceHashCache

    @Resource
    private lateinit var authRoleHashCache: AuthRoleHashCache

    @Resource
    private lateinit var roleIdsByUserIdCache: RoleIdsByUserIdCache

    @Resource
    private lateinit var resourceIdsByUserIdCache: ResourceIdsByUserIdCache

    @Resource
    private lateinit var eventPublisher: ApplicationEventPublisher

    // -- Below are injected for the aggregator methods (getEffectivePermissions, getDeleteImpact,
    //    batchBindUsers, copyRole). They cross resource boundaries (role↔group, role↔resource) on
    //    purpose; the alternative would be a separate "permissions facade" service, but the role
    //    service already mixes cross-MS concerns (e.g. sysResourceHashCache) so adding these stays
    //    in the same architectural style. None of these create a cycle: group services do not depend
    //    on the role service.

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var authRoleUserService: IAuthRoleUserService

    @Resource
    private lateinit var authRoleResourceService: IAuthRoleResourceService

    @Resource
    private lateinit var authGroupUserService: IAuthGroupUserService

    @Resource
    private lateinit var authGroupRoleService: IAuthGroupRoleService

    @Resource
    private lateinit var authGroupHashCache: AuthGroupHashCache

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getRoleUserIds(roleId: String): List<String> = userIdsByRoleIdCache.getUserIds(roleId)

    @Transactional(readOnly = true)
    override fun getRoleResourceIds(roleId: String): Set<String> =
        resourceIdsByRoleIdCache.getResourceIds(roleId).toSet()

    @Transactional(readOnly = true)
    override fun getRoleIds(tenantId: String): List<String> = dao.searchActiveRoleIdsByTenantId(tenantId)

    @Transactional(readOnly = true)
    override fun getRoleUsers(roleId: String): List<UserAccountCacheEntry> {
        val userIds = getRoleUserIds(roleId)
        if (userIds.isEmpty()) return emptyList()
        val usersMap = userAccountHashCache.getUsersByIds(userIds)
        return userIds.mapNotNull { usersMap[it] }
    }

    @Transactional(readOnly = true)
    override fun getRoleResources(roleId: String): List<SysResourceCacheEntry> {
        val resourceIds = getRoleResourceIds(roleId)
        if (resourceIds.isEmpty()) return emptyList()
        val resourcesMap = sysResourceHashCache.getResourcesByIds(resourceIds)
        return resourceIds.mapNotNull { resourcesMap[it] }
    }

    @Transactional(readOnly = true)
    override fun hasResource(roleId: String, resourceId: String): Boolean =
        resourceId in getRoleResourceIds(roleId)

    @Transactional(readOnly = true)
    override fun getRoleByTenantIdAndCode(tenantId: String, roleCode: String): AuthRoleCacheEntry? =
        authRoleHashCache.getRoleByTenantIdAndRoleCode(tenantId, roleCode)?.id
            ?.let { authRoleHashCache.getRoleById(it) }

    @Transactional(readOnly = true)
    override fun getRoleRecord(id: String): AuthRoleRow? = dao.getAs<AuthRoleRow>(id)

    @Transactional(readOnly = true)
    override fun getRolesByTenantId(tenantId: String): List<AuthRoleRow> =
        @Suppress("UNCHECKED_CAST")
        dao.search(AuthRoleQuery(tenantId = tenantId), AuthRoleRow::class)

    @Transactional(readOnly = true)
    override fun getRolesBySubsysCode(tenantId: String, subsysCode: String): List<AuthRoleRow> =
        @Suppress("UNCHECKED_CAST")
        dao.search(AuthRoleQuery(tenantId = tenantId, subsysCode = subsysCode), AuthRoleRow::class)

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val role = AuthRole.Companion {
            this.id = id
            this.active = active
        }
        val success = dao.update(role)
        if (success) {
            log.debug("Updated active status of role with id ${id} to ${active}.")
            eventPublisher.publishEvent(AuthRoleUpdated(id))
        } else {
            log.error("Failed to update active status of role with id ${id} to ${active}!")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        validateParentId(any, selfId = null)
        val id = super.insert(any)
        log.debug("Added role with id ${id}.")
        eventPublisher.publishEvent(AuthRoleInserted(id))
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = BeanKit.getProperty(any, AuthRole::id.name) as String
        validateParentId(any, selfId = id)
        val success = super.update(any)
        if (success) {
            log.debug("Updated role with id ${id}.")
            eventPublisher.publishEvent(AuthRoleUpdated(id))
        } else {
            log.error("Failed to update role with id ${id}!")
        }
        return success
    }

    /**
     * Guards against the three ways a bad parent_id can corrupt the inheritance graph: pointing
     * at self, pointing at a non-existent role, pointing at a role in a different tenant or
     * subsystem (cross-tenant inheritance is nonsensical), or closing a cycle by pointing at a
     * descendant.
     *
     * Skipped when the incoming `any` doesn't carry a parent_id at all (e.g. the activate-flag
     * sub-update via [updateActive] sets only `id` + `active`).
     */
    private fun validateParentId(any: Any, selfId: String?) {
        val parentId = try {
            BeanKit.getProperty(any, AuthRole::parentId.name) as? String
        } catch (_: Throwable) {
            return  // PO snippet without parent_id set — nothing to validate
        } ?: return  // parent_id explicitly null = root role; no constraints

        require(parentId.isNotBlank()) { "parent_id must not be blank; use null for a root role." }
        if (selfId != null) {
            require(parentId != selfId) { "A role cannot be its own parent (id=${selfId})." }
        }

        val parent = dao.get(parentId)
            ?: throw IllegalArgumentException("Parent role not found: $parentId")

        // Same-tenant + same-subsystem invariant. Inheritance across tenants would let one
        // tenant grant another's resources via a parent link, breaking isolation.
        val newRoleTenantId = BeanKit.getProperty(any, AuthRole::tenantId.name) as? String
        val newRoleSubsysCode = BeanKit.getProperty(any, AuthRole::subsysCode.name) as? String
        if (newRoleTenantId != null && newRoleTenantId != parent.tenantId) {
            throw IllegalArgumentException(
                "Parent role's tenant (${parent.tenantId}) does not match child's tenant ($newRoleTenantId)."
            )
        }
        if (newRoleSubsysCode != null && newRoleSubsysCode != parent.subsysCode) {
            throw IllegalArgumentException(
                "Parent role's subsystem (${parent.subsysCode}) does not match child's subsystem ($newRoleSubsysCode)."
            )
        }

        // Cycle: if the proposed parent has the role-being-updated anywhere in its ancestor chain,
        // OR if the role-being-updated has the proposed parent in its descendant set, then the
        // edge would close a loop. Only meaningful on update — a brand-new role has no descendants.
        if (selfId != null) {
            val proposedParentAncestors = dao.searchAncestorRoleIds(setOf(parentId))
            if (selfId in proposedParentAncestors) {
                throw IllegalArgumentException(
                    "Setting parent_id=$parentId on role $selfId would close a cycle."
                )
            }
            // Belt-and-braces: also check via descendants of self. Covers the case where the
            // proposed parent already exists in self's subtree.
            val selfDescendants = dao.searchDescendantRoleIds(selfId)
            if (parentId in selfDescendants) {
                throw IllegalArgumentException(
                    "Setting parent_id=$parentId on role $selfId would close a cycle (parent is a descendant)."
                )
            }
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val role = dao.get(id)
        if (role == null) {
            log.warn("Role with id ${id} no longer exists when attempting to delete!")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("Deleted role with id ${id}.")
            eventPublisher.publishEvent(AuthRoleDeleted(id, role.tenantId, role.code))
        } else {
            log.error("Failed to delete role with id ${id}!")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        // Snapshot tenantId/code first; after AFTER_COMMIT the rows are deleted and downstream (tenantId, code) caches
        // can no longer query back.
        val snapshots = if (ids.isEmpty()) emptyList()
            else dao.getByIds(ids).map { AuthRoleBatchDeleted.Item(it.id, it.tenantId, it.code) }
        val count = super.batchDelete(ids)
        log.debug("Batch delete roles: expected to delete ${ids.size} entries, actually deleted ${count}.")
        if (snapshots.isNotEmpty()) {
            eventPublisher.publishEvent(AuthRoleBatchDeleted(snapshots))
        }
        return count
    }

    @Transactional(readOnly = true)
    override fun hasRole(userId: String, roleId: String): Boolean = roleId in getUserRoleIds(userId)

    @Transactional(readOnly = true)
    override fun hasRoleByCode(userId: String, tenantId: String, roleCode: String): Boolean =
        authRoleHashCache.getRoleByTenantIdAndRoleCode(tenantId, roleCode)?.id
            ?.let { hasRole(userId, it) } == true

    @Transactional(readOnly = true)
    override fun getUserRoles(userId: String): List<AuthRoleCacheEntry> {
        val roleIds = getUserRoleIds(userId)
        if (roleIds.isEmpty()) return emptyList()
        val rolesMap = authRoleHashCache.getRolesByIds(roleIds)
        return roleIds.mapNotNull { rolesMap[it] }
    }

    @Transactional(readOnly = true)
    override fun getUserRoleIds(userId: String): List<String> = roleIdsByUserIdCache.getRoleIds(userId)

    @Transactional(readOnly = true)
    override fun getUsersByRoleCode(tenantId: String, roleCode: String): List<UserAccountCacheEntry> {
        val roleId = authRoleHashCache.getRoleByTenantIdAndRoleCode(tenantId, roleCode)?.id ?: return emptyList()
        val userIds = userIdsByRoleIdCache.getUserIds(roleId)
        if (userIds.isEmpty()) return emptyList()
        return userAccountHashCache.getUsersByIds(userIds).values.toList()
    }

    @Transactional(readOnly = true)
    override fun isUserHasResource(userId: String, resourceId: String): Boolean =
        resourceId in getUserResourceIds(userId)

    @Transactional(readOnly = true)
    override fun getUserResourceIds(userId: String): Set<String> =
        resourceIdsByUserIdCache.getResourceIds(userId).toSet()

    @Transactional(readOnly = true)
    override fun getResources(userId: String): List<SysResourceCacheEntry> {
        // Get the list of resource IDs by user ID
        val resourceIds = resourceIdsByUserIdCache.getResourceIds(userId)
        if (resourceIds.isEmpty()) return emptyList()
        // Batch fetch resource cache objects (sysResourceHashCache.getResourcesByIds accepts a Set, perform a dedup conversion)
        val resourcesMap = sysResourceHashCache.getResourcesByIds(resourceIds.toSet())
        // Return resource list (in original ID order)
        return resourceIds.mapNotNull { resourcesMap[it] }
    }

    // -----------------------------------------------------------------------
    // Aggregators — replace front-end N+1 fan-outs with one-trip composite calls.
    // -----------------------------------------------------------------------

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    override fun getEffectivePermissions(userId: String): EffectivePermissionsVo {
        // Direct role IDs: bypass IAuthRoleUserService.getRoleIdsByUserId because that's actually
        // the cached *effective* set (it includes group-inherited roles). The dao gives the bare
        // role_user table — which is what "direct" means.
        val directRoleIds: List<String> = authRoleUserDao.searchRoleIdsByUserId(userId)
        val groupIds: Set<String> = authGroupUserService.getGroupIdsByUserId(userId)

        if (directRoleIds.isEmpty() && groupIds.isEmpty()) return EffectivePermissionsVo.empty()

        // groupId -> role IDs inherited via that group (deduplicated by Set on the read side).
        val roleIdsByGroup: Map<String, Set<String>> = groupIds.associateWith { gid ->
            authGroupRoleService.getRoleIdsByGroupId(gid)
        }

        // Resolve all role metadata once (direct + inherited, dedup), then partition.
        val allRoleIds: Set<String> = (directRoleIds.asSequence() + roleIdsByGroup.values.asSequence().flatten()).toSet()
        val rolesMap: Map<String, AuthRoleCacheEntry> =
            if (allRoleIds.isEmpty()) emptyMap() else authRoleHashCache.getRolesByIds(allRoleIds)

        val directRoles: List<AuthRoleCacheEntry> = directRoleIds.mapNotNull { rolesMap[it] }
        val groupsList: List<AuthGroupCacheEntryAlias> =
            if (groupIds.isEmpty()) emptyList() else authGroupHashCache.getGroupsByIds(groupIds).values.toList()
        val rolesByGroup: Map<String, List<AuthRoleCacheEntry>> =
            roleIdsByGroup.mapValues { (_, rids) -> rids.mapNotNull { rolesMap[it] } }

        // Resources per role: one cache hit per role for the id list, one batch lookup for metadata.
        // Empty roles are pruned so the JSON doesn't carry useless `roleId: []` pairs.
        val resourcesByRole: Map<String, List<SysResourceCacheEntry>> = allRoleIds.associateWith { rid ->
            val resIds = resourceIdsByRoleIdCache.getResourceIds(rid)
            if (resIds.isEmpty()) emptyList() else {
                val map = sysResourceHashCache.getResourcesByIds(resIds.toSet())
                resIds.mapNotNull { map[it] }
            }
        }.filterValues { it.isNotEmpty() }

        return EffectivePermissionsVo(
            directRoles = directRoles,
            groups = groupsList,
            rolesByGroup = rolesByGroup,
            resourcesByRole = resourcesByRole,
        )
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    override fun getDeleteImpact(roleIds: Collection<String>): RoleDeleteImpactVo {
        if (roleIds.isEmpty()) return RoleDeleteImpactVo.zero()
        // Union over all roles. Sets de-duplicate so a user/group bound to multiple roles in the
        // batch counts once — matches the UI's "this batch's blast radius" phrasing.
        val allUserIds = HashSet<String>()
        val allGroupIds = HashSet<String>()
        for (rid in roleIds) {
            allUserIds.addAll(authRoleUserService.getUserIdsByRoleId(rid))
            allGroupIds.addAll(authGroupRoleService.getGroupIdsByRoleId(rid))
        }
        return RoleDeleteImpactVo(users = allUserIds.size, groups = allGroupIds.size)
    }

    @Transactional
    override fun batchBindUsers(roleIds: Collection<String>, userIds: Collection<String>): BatchBindResultVo {
        if (roleIds.isEmpty() || userIds.isEmpty()) return BatchBindResultVo.empty()
        var ok = 0
        val failures = mutableListOf<BatchBindResultVo.BatchBindFailure>()
        // Per-role transaction boundary: each batchBind is its own commit so a bad row doesn't
        // strand the rest. The admin UI displays partial-failure detail; cross-role atomicity
        // isn't worth the multi-row write lock.
        for (rid in roleIds) {
            try {
                authRoleUserService.batchBind(rid, userIds)
                ok++
            } catch (e: Exception) {
                log.warn("Batch-bind failed for role ${rid}: ${e.message}")
                failures += BatchBindResultVo.BatchBindFailure(ownerId = rid, reason = e.message ?: e.javaClass.simpleName)
            }
        }
        return BatchBindResultVo(ok = ok, failures = failures)
    }

    @Transactional
    override fun copyRole(sourceId: String, code: String, name: String, copyResources: Boolean): String {
        require(code.isNotBlank()) { "Target role code must not be blank" }
        require(name.isNotBlank()) { "Target role name must not be blank" }
        val source = authRoleHashCache.getRoleById(sourceId)
            ?: throw IllegalArgumentException("Source role not found: $sourceId")

        // Build the new PO from the source's audit-neutral fields. Audit columns (createUserId
        // etc.) are left null so BaseCrudService's audit interceptor stamps the current operator.
        val newRole = AuthRole.Companion {
            this.code = code
            this.name = name
            // Required non-null fields on the PO; the cache entry's tenantId / subsysCode are
            // nullable so we fall back to empty-string when (somehow) absent rather than NPE.
            this.tenantId = source.tenantId ?: ""
            this.subsysCode = source.subsysCode ?: ""
            this.remark = source.remark
            this.active = source.active ?: true
            this.builtIn = false  // copies are never built-in regardless of source
        }
        val newId: String = dao.insert(newRole)
        log.debug("Copied role ${sourceId} -> ${newId} (copyResources=${copyResources})")
        eventPublisher.publishEvent(AuthRoleInserted(newId))

        if (copyResources) {
            val resourceIds = authRoleResourceService.getResourceIdsByRoleId(sourceId)
            if (resourceIds.isNotEmpty()) {
                authRoleResourceService.batchBind(newId, resourceIds)
                log.debug("Copied ${resourceIds.size} resource grants from role ${sourceId} to ${newId}.")
            }
        }
        return newId
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    override fun getAncestorRoleIds(roleId: String): List<String> {
        // Walk root → ... → direct parent. The dao returns an unordered set; sort by depth-from-root
        // by re-walking from the role itself one hop at a time.
        val ancestors = dao.searchAncestorRoleIds(setOf(roleId))
        if (ancestors.isEmpty()) return emptyList()
        // Order: direct parent first, then grandparent, etc. — that's the most intuitive for the
        // admin UI's chain display.
        val ordered = mutableListOf<String>()
        var current: String? = roleId
        val cap = 64
        var depth = 0
        while (current != null && depth < cap) {
            val parentEntry = dao.get(current) ?: break
            val parent = parentEntry.parentId ?: break
            if (parent in ordered) break // cycle defense
            ordered += parent
            current = parent
            depth++
        }
        return ordered
    }

}

/** Local alias so the type appears in the import block above for clarity. */
private typealias AuthGroupCacheEntryAlias = io.kudos.ms.auth.common.group.vo.AuthGroupCacheEntry
