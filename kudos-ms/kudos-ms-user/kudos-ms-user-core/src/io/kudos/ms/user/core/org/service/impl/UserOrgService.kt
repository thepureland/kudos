package io.kudos.ms.user.core.org.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.query.PagingSearchResult
import io.kudos.ms.user.common.org.vo.request.UserOrgQuery
import io.kudos.ms.user.core.security.UserAccessScope
import io.kudos.ms.user.core.security.UserTenantAccessGuard
import io.kudos.ms.user.core.security.UserTenantAccessGuard.Companion.ownerKey
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.core.tenant.dao.SysTenantDao
import io.kudos.ms.sys.core.tenant.model.po.SysTenant
import org.ktorm.entity.Entity
import java.util.UUID
import kotlin.reflect.KClass
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.common.org.vo.response.UserOrgTreeRow
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.user.core.org.cache.UserIdsByOrgIdCache
import io.kudos.ms.user.core.org.cache.UserOrgHashCache
import io.kudos.ms.user.core.org.dao.UserOrgDao
import io.kudos.ms.user.core.account.dao.UserOrgUserDao
import io.kudos.ms.user.core.org.event.UserOrgBatchDeleted
import io.kudos.ms.user.core.org.event.UserOrgDeleted
import io.kudos.ms.user.core.org.event.UserOrgInserted
import io.kudos.ms.user.core.org.event.UserOrgUpdated
import io.kudos.ms.user.core.org.model.po.UserOrg
import io.kudos.ms.user.core.org.service.iservice.IUserOrgService
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * Organization business.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class UserOrgService(
    dao: UserOrgDao
) : BaseCrudService<String, UserOrg, UserOrgDao>(dao), IUserOrgService {


    @Autowired
    private lateinit var userOrgUserDao: UserOrgUserDao

    @Autowired
    private lateinit var userOrgHashCache: UserOrgHashCache

    @Resource
    private lateinit var userAccountHashCache: UserAccountHashCache

    @Autowired
    private lateinit var userIdsByOrgIdCache: UserIdsByOrgIdCache

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    private var tenantAccess = UserTenantAccessGuard()

    /** Organization-mode checks; absent only in pure unit tests, where only legacy rows exist. */
    @Autowired(required = false)
    private var directoryGate: OrganizationDirectoryGate? = null

    @Autowired(required = false)
    private var userAccountDao: UserAccountDao? = null

    @Autowired(required = false)
    private var sysTenantDao: SysTenantDao? = null

    private fun gate(): OrganizationDirectoryGate = requireNotNull(directoryGate) { "ORGANIZATION_MODE_UNAVAILABLE" }

    private fun accessibleOrg(id: String): UserOrg? =
        dao.get(id)?.also { if (tenantAccess.hasPrincipal()) tenantAccess.assertCanAccessOwner(it.tenantId, it.organizationId) }

    /** Organization rows need the directory policy for every write; legacy rows keep the old rules. */
    private fun assertCanWrite(org: UserOrg) {
        val organizationId = org.organizationId ?: return
        if (org.isOrganizationRoot()) gate().assertPlatform() else gate().assertCanManageDirectory(organizationId)
    }

    private fun stringProperty(value: Any, name: String): String? =
        runCatching { BeanKit.getProperty(value, name) as? String }.getOrNull()

    /**
     * Parent and every ancestor share the child's owner, and the chain has no cycle. Legacy rows are
     * checked only for authenticated callers, as before; organization rows always.
     */
    private fun assertParent(parentId: String?, tenantId: String, movingId: String? = null, organizationId: String? = null) {
        if (parentId.isNullOrBlank() || (organizationId == null && !tenantAccess.hasPrincipal())) return
        val parent = requireNotNull(accessibleOrg(parentId)) { "Parent organization not found" }
        val owner = ownerKey(tenantId, organizationId)
        require(ownerKey(parent.tenantId, parent.organizationId) == owner) {
            if (organizationId == null) "Parent and child organizations must belong to the same tenant"
            else "Parent and child departments must belong to the same organization"
        }
        val visited = mutableSetOf<String>()
        var ancestor: UserOrg? = parent
        while (ancestor != null) {
            val id = ancestor.id
            require(id != movingId && visited.add(id)) { "Organization hierarchy cannot contain a cycle" }
            require(ownerKey(ancestor.tenantId, ancestor.organizationId) == owner) { "Organization ancestry crosses tenants" }
            ancestor = ancestor.parentId?.takeIf(String::isNotBlank)?.let { dao.get(it) }
        }
    }

    @Transactional(readOnly = true)
    override fun get(id: String): UserOrg? = accessibleOrg(id)

    @Transactional(readOnly = true)
    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        if (tenantAccess.hasPrincipal()) accessibleOrg(id) ?: return null
        return dao.get(id, returnType)
    }

    @Transactional(readOnly = true)
    override fun pagingSearch(listSearchPayload: ListSearchPayload): PagingSearchResult<*> {
        val scope = tenantAccess.restrictedScope()
        if (scope is UserAccessScope.Organization) {
            require(listSearchPayload is UserOrgQuery) { "An organization-scoped query is required" }
            require(listSearchPayload.organizationId.isNullOrBlank() || listSearchPayload.organizationId == scope.organizationId) {
                "Cross-organization user administration is forbidden"
            }
            val scoped = listSearchPayload.copy(organizationId = scope.organizationId, tenantId = null).apply {
                pageNo = listSearchPayload.pageNo
                pageSize = listSearchPayload.pageSize
                orders = listSearchPayload.orders
            }
            scoped.parentId?.takeIf(String::isNotBlank)?.let { assertParent(it, "", organizationId = scope.organizationId) }
            return super.pagingSearch(scoped)
        }
        val restricted = (scope as? UserAccessScope.Tenant)?.tenantId ?: return super.pagingSearch(listSearchPayload)
        require(listSearchPayload is UserOrgQuery) { "A tenant-scoped organization query is required" }
        val scoped = listSearchPayload.copy(tenantId = tenantAccess.queryTenantId(listSearchPayload.tenantId)).apply {
            pageNo = listSearchPayload.pageNo
            pageSize = listSearchPayload.pageSize
            orders = listSearchPayload.orders
        }
        scoped.parentId?.takeIf(String::isNotBlank)?.let { assertParent(it, restricted) }
        return super.pagingSearch(scoped)
    }

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getOrgAdmins(orgId: String): List<UserAccountCacheEntry> {
        val org = if (tenantAccess.hasPrincipal()) accessibleOrg(orgId) ?: return emptyList() else null
        val adminUserIds = userOrgUserDao.searchAdminUserIdsByOrgId(orgId)
        if (adminUserIds.isEmpty()) return emptyList()
        // Batch fetch user info, returned in original ID order.
        val usersMap = userAccountHashCache.getUsersByIds(adminUserIds)
        return adminUserIds.mapNotNull { usersMap[it] }.filter { org == null || sameOwner(org, it) }
    }

    @Transactional(readOnly = true)
    override fun getOrgUserIds(orgId: String): List<String> {
        if (tenantAccess.hasPrincipal()) accessibleOrg(orgId) ?: return emptyList()
        return userIdsByOrgIdCache.getUserIds(orgId)
    }

    @Transactional(readOnly = true)
    override fun getChildOrgIds(orgId: String): List<String> {
        if (tenantAccess.hasPrincipal()) accessibleOrg(orgId) ?: return emptyList()
        return dao.searchActiveChildOrgIds(orgId)
    }

    @Transactional(readOnly = true)
    override fun getOrgUsers(orgId: String): List<UserAccountCacheEntry> {
        val org = if (tenantAccess.hasPrincipal()) accessibleOrg(orgId) ?: return emptyList() else null
        val userIds = getOrgUserIds(orgId)
        if (userIds.isEmpty()) return emptyList()
        val usersMap = userAccountHashCache.getUsersByIds(userIds)
        return userIds.mapNotNull { usersMap[it] }.filter { org == null || sameOwner(org, it) }
    }

    @Transactional(readOnly = true)
    override fun isUserInOrg(userId: String, orgId: String): Boolean = userId in getOrgUserIds(orgId)

    @Transactional(readOnly = true)
    override fun getChildOrgs(orgId: String): List<UserOrgCacheEntry> {
        val childOrgIds = getChildOrgIds(orgId)
        if (childOrgIds.isEmpty()) return emptyList()
        val orgsMap = userOrgHashCache.getOrgsByIds(childOrgIds)
        return childOrgIds.mapNotNull { orgsMap[it] }
    }

    @Transactional(readOnly = true)
    override fun getParentOrg(orgId: String): UserOrgCacheEntry? {
        val org = getOrgRecord(orgId) ?: return null
        return org.parentId?.let { getOrgRecord(it) }
            ?.takeIf { ownerKey(it.tenantId, it.organizationId) == ownerKey(org.tenantId, org.organizationId) }
    }

    @Transactional(readOnly = true)
    override fun getOrgRecord(id: String): UserOrgCacheEntry? {
        if (tenantAccess.hasPrincipal()) accessibleOrg(id) ?: return null
        return userOrgHashCache.getOrgById(id)
    }

    @Transactional(readOnly = true)
    override fun getOrgsByTenantId(tenantId: String): List<UserOrgCacheEntry> {
        tenantAccess.assertCanAccess(tenantId)
        val orgIds = userOrgHashCache.getOrgsByTenantId(tenantId).map { it.id }
        if (orgIds.isEmpty()) return emptyList()
        val orgsMap = userOrgHashCache.getOrgsByIds(orgIds)
        return orgIds.mapNotNull { orgsMap[it] }
    }

    @Transactional(readOnly = true)
    override fun getOrgsByOrganizationId(organizationId: String): List<UserOrgCacheEntry> {
        tenantAccess.assertCanAccessOrganization(organizationId)
        return userOrgHashCache.getOrgsByOrganizationId(organizationId)
    }

    @Transactional(readOnly = true)
    override fun getOrgTreeByOrganizationId(organizationId: String, parentId: String?): List<UserOrgTreeRow> {
        tenantAccess.assertCanAccessOrganization(organizationId)
        assertParent(parentId, "", organizationId = organizationId)
        return buildTree(dao.searchActiveOrgsByOrganizationId(organizationId, parentId), parentId)
    }

    @Transactional(readOnly = true)
    override fun getOrgTree(tenantId: String, parentId: String?): List<UserOrgTreeRow> {
        tenantAccess.assertCanAccess(tenantId)
        assertParent(parentId, tenantId)
        // If parentId is specified, only query direct child organizations under that parent organization;
        // otherwise query all enabled organizations under the tenant.
        return buildTree(dao.searchActiveOrgsByTenantId(tenantId, parentId), parentId)
    }

    private fun buildTree(orgs: List<UserOrg>, parentId: String?): List<UserOrgTreeRow> {

        // Convert to tree nodes (UserOrgTreeRow is an immutable data class with all val properties,
        // use constructor to pass values; BeanKit.copyProperties uses setters and is unusable here, would leave an empty row).
        val treeNodes = orgs.mapNotNull { org ->
            val cacheItem = userOrgHashCache.getOrgById(org.id) ?: return@mapNotNull null
            UserOrgTreeRow(
                id = cacheItem.id,
                name = cacheItem.name,
                shortName = cacheItem.shortName,
                tenantId = cacheItem.tenantId,
                parentId = cacheItem.parentId,
                orgTypeDictCode = cacheItem.orgTypeDictCode,
                sortNum = cacheItem.sortNum,
                remark = cacheItem.remark,
                active = cacheItem.active,
                builtIn = cacheItem.builtIn,
                createUserId = cacheItem.createUserId,
                createUserName = cacheItem.createUserName,
                createTime = cacheItem.createTime,
                updateUserId = cacheItem.updateUserId,
                updateUserName = cacheItem.updateUserName,
                updateTime = cacheItem.updateTime,
                organizationId = cacheItem.organizationId,
                nodeKind = cacheItem.nodeKind,
                children = mutableListOf(),
            )
        }
        
        // If parentId is specified, directly return the child organization list (do not build a tree).
        if (parentId != null) {
            return treeNodes.sortedBy { it.sortNum ?: Int.MAX_VALUE }
        }

        // Build the tree structure (only when parentId == null).
        val nodeMap = treeNodes.associateBy { it.id }
        val rootNodes = mutableListOf<UserOrgTreeRow>()
        
        treeNodes.forEach { node ->
            if (node.parentId == null) {
                rootNodes.add(node)
            } else {
                val parent = nodeMap[node.parentId]
                parent?.children?.add(node)
            }
        }
        
        // Sort by sortNum.
        fun sortTree(nodes: MutableList<UserOrgTreeRow>) {
            nodes.sortBy { it.sortNum ?: Int.MAX_VALUE }
            nodes.forEach { node ->
                node.children?.let { sortTree(it) }
            }
        }
        sortTree(rootNodes)
        
        return rootNodes
    }

    @Transactional(readOnly = true)
    override fun getAllAncestorOrgIds(orgId: String): List<String> {
        if (tenantAccess.hasPrincipal()) accessibleOrg(orgId) ?: return emptyList()
        val visited = mutableSetOf(orgId)
        val ancestors = mutableListOf<String>()
        var currentOrg = userOrgHashCache.getOrgById(orgId) ?: return emptyList()
        while (true) {
            val parentId = currentOrg.parentId ?: break
            if (!visited.add(parentId)) break
            if (tenantAccess.hasPrincipal()) accessibleOrg(parentId) ?: break
            ancestors.add(parentId)
            // When the cache misses, still keep the parentId just added (ancestor chain may span cache boundaries, cannot truncate the whole segment).
            currentOrg = userOrgHashCache.getOrgById(parentId) ?: break
        }
        return ancestors
    }

    @Transactional(readOnly = true)
    override fun getAllDescendantOrgIds(orgId: String): List<String> {
        if (tenantAccess.hasPrincipal()) accessibleOrg(orgId) ?: return emptyList()
        val descendants = mutableListOf<String>()
        val visited = mutableSetOf(orgId)
        // ArrayDeque.removeFirst is O(1); avoids the O(n) shift each time with MutableList.removeAt(0).
        val queue = ArrayDeque(listOf(orgId))
        while (queue.isNotEmpty()) {
            val childIds = getChildOrgIds(queue.removeFirst()).filter { visited.add(it) }
            descendants.addAll(childIds)
            queue.addAll(childIds)
        }
        return descendants
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        // Changing active affects the "parent organization includes child tree members" view
        // (child organization disabled -> this subtree should be empty in the parent view).
        // Therefore, even if parentId is unchanged, the parentId snapshot must be put into the event
        // so the listener can clear caches along the ancestor chain.
        val org = accessibleOrg(id) ?: return false
        assertCanWrite(org)
        val parentId = org.parentId
        val success = dao.updateProperties(id, mapOf(UserOrg::active.name to active))
        if (success) {
            log.debug("Updated the enabled status of the organization with id ${id} to ${active}.")
            eventPublisher.publishEvent(UserOrgUpdated(id, oldParentId = parentId, newParentId = parentId))
        } else {
            log.error("Failed to update the enabled status of the organization with id ${id} to ${active}!")
        }
        return success
    }

    @Transactional
    override fun moveOrg(id: String, newParentId: String?, newSortNum: Int?): Boolean {
        // Snapshot oldParentId before moving -- after the transaction commits, the dao cannot see the old value.
        val org = accessibleOrg(id) ?: return false
        assertCanWrite(org)
        // An organization root never moves; a department moved to "no parent" goes under its root.
        require(!org.isOrganizationRoot()) { "An organization root cannot be moved" }
        val targetParentId = if (org.organizationId != null) newParentId?.takeIf(String::isNotBlank) ?: org.organizationId else newParentId
        if (tenantAccess.hasPrincipal() || org.organizationId != null) assertParent(targetParentId, org.tenantId, id, org.organizationId)
        val oldParentId = org.parentId
        val props = mutableMapOf<String, Any?>(UserOrg::parentId.name to targetParentId)
        newSortNum?.let { props[UserOrg::sortNum.name] = it }
        val success = dao.updateProperties(id, props)
        if (success) {
            log.debug("Moved the organization with id ${id} to parent organization ${newParentId}, sort number ${newSortNum}.")
            eventPublisher.publishEvent(UserOrgUpdated(id, oldParentId = oldParentId, newParentId = targetParentId))
        } else {
            log.error("Failed to move the organization with id ${id}!")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val nodeKind = stringProperty(any, "nodeKind")?.takeIf(String::isNotBlank)
        val parentId = stringProperty(any, "parentId")?.takeIf(String::isNotBlank)
        val parent = parentId?.let { dao.get(it) }
        val scope = tenantAccess.restrictedScope()
        val organizationId = stringProperty(any, "organizationId")?.takeIf(String::isNotBlank)
            ?: parent?.organizationId
            ?: (scope as? UserAccessScope.Organization)?.organizationId
        val id = when {
            nodeKind == UserOrgNodeKind.ORGANIZATION -> insertOrganizationRoot(any, parentId)
            nodeKind == UserOrgNodeKind.DEPARTMENT || organizationId != null ->
                insertDepartment(any, requireNotNull(organizationId) { "Organization is required" }, parentId)
            else -> {
                val tenantId = stringProperty(any, "tenantId")
                directoryGate?.assertLegacyTenantAllowed(tenantId)
                if (tenantAccess.hasPrincipal()) {
                    tenantAccess.assertCanAccess(requireNotNull(tenantId) { "Tenant is required" })
                    assertParent(parentId, tenantId)
                }
                super.insert(any)
            }
        }
        log.debug("Added the organization with id ${id}.")
        eventPublisher.publishEvent(UserOrgInserted(id))
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        // Generic update: snapshot pre-update parentId, then read post-update parentId after update.
        val id = BeanKit.getProperty(any, UserOrg::id.name) as String
        val org = accessibleOrg(id) ?: return false
        val requestedTenantId = stringProperty(any, "tenantId")
        val organizationId = org.organizationId
        if (organizationId == null) {
            require(requestedTenantId == null || requestedTenantId == org.tenantId) {
                "An organization cannot be moved to another tenant"
            }
            // A legacy node never becomes an organization node: ownership is decided at creation only.
            require(stringProperty(any, "organizationId").isNullOrBlank() && stringProperty(any, "nodeKind").isNullOrBlank()) {
                "A tenant-owned organization cannot be moved into a customer organization"
            }
            if (tenantAccess.hasPrincipal()) assertParent(stringProperty(any, "parentId"), org.tenantId, id)
        } else {
            assertCanWrite(org)
            require(stringProperty(any, "organizationId").let { it.isNullOrBlank() || it == organizationId }) {
                "A department cannot be moved to another organization"
            }
            require(stringProperty(any, "nodeKind").let { it.isNullOrBlank() || it == org.nodeKind }) { "Node kind is immutable" }
        }
        val oldParentId = org.parentId
        val success = if (organizationId == null) super.update(any) else dao.update(pinnedOwnership(any, org))
        if (success) {
            val newParentId = dao.get(id)?.parentId
            log.debug("Updated the organization with id ${id}.")
            eventPublisher.publishEvent(UserOrgUpdated(id, oldParentId = oldParentId, newParentId = newParentId))
        } else {
            log.error("Failed to update the organization with id ${id}!")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val org = accessibleOrg(id) ?: run {
            log.warn("When deleting the organization with id ${id}, found it no longer exists!")
            return false
        }
        assertDeletable(org)
        val parentIdSnapshot = org.parentId
        val success = super.deleteById(id)
        if (success) {
            log.debug("Deleted the organization with id ${id}.")
            eventPublisher.publishEvent(UserOrgDeleted(id, parentId = parentIdSnapshot))
        } else {
            log.error("Failed to delete the organization with id ${id}!")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        // First snapshot (id, parentId); at AFTER_COMMIT the rows have been deleted, the listener cannot query back.
        val snapshots = if (ids.isEmpty()) emptyList()
            else dao.getByIds(ids).map {
                if (tenantAccess.hasPrincipal()) tenantAccess.assertCanAccessOwner(it.tenantId, it.organizationId)
                assertDeletable(it)
                UserOrgBatchDeleted.Item(it.id, it.parentId)
            }
        val count = super.batchDelete(ids)
        log.debug("Batch deleted organizations, expected to delete ${ids.size} records, actually deleted ${count} records.")
        if (snapshots.isNotEmpty()) {
            eventPublisher.publishEvent(UserOrgBatchDeleted(snapshots))
        }
        return count
    }



    /** A customer organization: a self-owned root without tenant or parent. Platform only. */
    private fun insertOrganizationRoot(any: Any, parentId: String?): String {
        gate().assertPlatform()
        require(parentId == null) { "An organization root has no parent" }
        val root = Entity.create<UserOrg>()
        BeanKit.copyProperties(any, root)
        val id = UUID.randomUUID().toString()
        root.id = id
        root.organizationId = id
        root.nodeKind = UserOrgNodeKind.ORGANIZATION
        root.tenantId = ""
        root.parentId = null
        return dao.insert(root)
    }

    /** A department of [organizationId]; with no parent it hangs directly under the root. */
    private fun insertDepartment(any: Any, organizationId: String, parentId: String?): String {
        val root = requireNotNull(dao.get(organizationId)?.takeIf { it.isOrganizationRoot() }) { "ORGANIZATION_NOT_FOUND" }
        require(root.active) { "ORGANIZATION_DISABLED" }
        gate().assertCanManageDirectory(organizationId)
        val targetParentId = parentId ?: organizationId
        assertParent(targetParentId, "", organizationId = organizationId)
        val department = Entity.create<UserOrg>()
        BeanKit.copyProperties(any, department)
        department.organizationId = organizationId
        department.nodeKind = UserOrgNodeKind.DEPARTMENT
        department.tenantId = ""
        department.parentId = targetParentId
        return dao.insert(department)
    }

    /** Organization nodes still in use are not deleted; legacy deletes keep their old behaviour. */
    private fun assertDeletable(org: UserOrg) {
        val organizationId = org.organizationId ?: return
        assertCanWrite(org)
        require(dao.searchActiveChildOrgIds(org.id).isEmpty() && dao.search(Criteria(UserOrg::parentId eq org.id)).isEmpty()) {
            "A department with sub-departments cannot be deleted"
        }
        require(userOrgUserDao.searchUserIdsByOrgId(org.id).isEmpty()) { "A department with members cannot be deleted" }
        if (org.isOrganizationRoot()) {
            require(userAccountDao?.search(Criteria(io.kudos.ms.user.core.account.model.po.UserAccount::organizationId eq organizationId))
                .isNullOrEmpty()) { "An organization with accounts cannot be deleted" }
            require(sysTenantDao?.search(Criteria(SysTenant::organizationId eq organizationId)).isNullOrEmpty()) {
                "An organization that owns tenants cannot be deleted"
            }
        }
    }

    /**
     * An update entity for an organization node whose ownership columns cannot be changed or wiped by
     * the form: the generic update copies every form property, nulls included.
     */
    private fun pinnedOwnership(any: Any, existing: UserOrg): UserOrg {
        val entity = Entity.create<UserOrg>()
        BeanKit.copyProperties(any, entity)
        entity.id = existing.id
        entity.organizationId = existing.organizationId
        entity.nodeKind = existing.nodeKind
        entity.tenantId = existing.tenantId
        val requestedParent = stringProperty(any, "parentId")?.takeIf(String::isNotBlank)
        entity.parentId = if (existing.isOrganizationRoot()) null else requestedParent ?: existing.parentId
        if (!existing.isOrganizationRoot()) assertParent(entity.parentId, "", existing.id, existing.organizationId)
        return entity
    }

    private fun sameOwner(org: UserOrg, account: UserAccountCacheEntry): Boolean =
        ownerKey(account.tenantId, account.organizationId) == ownerKey(org.tenantId, org.organizationId)

}
