package io.kudos.ms.user.core.org.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.query.PagingSearchResult
import io.kudos.ms.user.common.org.vo.request.UserOrgQuery
import io.kudos.ms.user.core.security.UserTenantAccessGuard
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

    private fun accessibleOrg(id: String): UserOrg? =
        dao.get(id)?.also { if (tenantAccess.hasPrincipal()) tenantAccess.assertCanAccess(it.tenantId) }

    private fun stringProperty(value: Any, name: String): String? =
        runCatching { BeanKit.getProperty(value, name) as? String }.getOrNull()

    private fun assertParent(parentId: String?, tenantId: String, movingId: String? = null) {
        if (!tenantAccess.hasPrincipal() || parentId.isNullOrBlank()) return
        val parent = requireNotNull(accessibleOrg(parentId)) { "Parent organization not found" }
        require(parent.tenantId == tenantId) { "Parent and child organizations must belong to the same tenant" }
        val visited = mutableSetOf<String>()
        var ancestor: UserOrg? = parent
        while (ancestor != null) {
            val id = ancestor.id
            require(id != movingId && visited.add(id)) { "Organization hierarchy cannot contain a cycle" }
            require(ancestor.tenantId == tenantId) { "Organization ancestry crosses tenants" }
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
        val restricted = tenantAccess.restrictedTenantId()
        if (restricted == null) return super.pagingSearch(listSearchPayload)
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
        return adminUserIds.mapNotNull { usersMap[it] }.filter { org == null || it.tenantId == org.tenantId }
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
        return userIds.mapNotNull { usersMap[it] }.filter { org == null || it.tenantId == org.tenantId }
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
        return org.parentId?.let { getOrgRecord(it) }?.takeIf { it.tenantId == org.tenantId }
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
    override fun getOrgTree(tenantId: String, parentId: String?): List<UserOrgTreeRow> {
        tenantAccess.assertCanAccess(tenantId)
        assertParent(parentId, tenantId)
        // If parentId is specified, only query direct child organizations under that parent organization;
        // otherwise query all enabled organizations under the tenant.
        val orgs = dao.searchActiveOrgsByTenantId(tenantId, parentId)

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
        val parentId = (accessibleOrg(id) ?: return false).parentId
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
        if (tenantAccess.hasPrincipal()) assertParent(newParentId, org.tenantId, id)
        val oldParentId = org.parentId
        val props = mutableMapOf<String, Any?>(UserOrg::parentId.name to newParentId)
        newSortNum?.let { props[UserOrg::sortNum.name] = it }
        val success = dao.updateProperties(id, props)
        if (success) {
            log.debug("Moved the organization with id ${id} to parent organization ${newParentId}, sort number ${newSortNum}.")
            eventPublisher.publishEvent(UserOrgUpdated(id, oldParentId = oldParentId, newParentId = newParentId))
        } else {
            log.error("Failed to move the organization with id ${id}!")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        if (tenantAccess.hasPrincipal()) {
            val tenantId = requireNotNull(stringProperty(any, "tenantId")) { "Tenant is required" }
            tenantAccess.assertCanAccess(tenantId)
            assertParent(stringProperty(any, "parentId"), tenantId)
        }
        val id = super.insert(any)
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
        require(requestedTenantId == null || requestedTenantId == org.tenantId) {
            "An organization cannot be moved to another tenant"
        }
        if (tenantAccess.hasPrincipal()) assertParent(stringProperty(any, "parentId"), org.tenantId, id)
        val oldParentId = org.parentId
        val success = super.update(any)
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
                if (tenantAccess.hasPrincipal()) tenantAccess.assertCanAccess(it.tenantId)
                UserOrgBatchDeleted.Item(it.id, it.parentId)
            }
        val count = super.batchDelete(ids)
        log.debug("Batch deleted organizations, expected to delete ${ids.size} records, actually deleted ${count} records.")
        if (snapshots.isNotEmpty()) {
            eventPublisher.publishEvent(UserOrgBatchDeleted(snapshots))
        }
        return count
    }


}
