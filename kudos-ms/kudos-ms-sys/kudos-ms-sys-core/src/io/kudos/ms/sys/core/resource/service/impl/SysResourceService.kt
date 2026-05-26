package io.kudos.ms.sys.core.resource.service.impl

import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.platform.service.impl.requireStringId
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.ms.sys.common.platform.consts.SysConsts
import io.kudos.ms.sys.common.platform.consts.SysDictTypes
import io.kudos.ms.sys.common.resource.enums.ResourceTypeEnum
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.sys.common.resource.vo.request.SysResourceQuery
import io.kudos.ms.sys.common.resource.vo.response.BaseMenuTreeNode
import io.kudos.ms.sys.common.resource.vo.response.MenuTreeNode
import io.kudos.ms.sys.common.resource.vo.response.SysResourceDetail
import io.kudos.ms.sys.common.resource.vo.response.SysResourceRow
import io.kudos.ms.sys.common.resource.vo.response.SysResourceTreeRow
import io.kudos.ms.sys.core.dict.cache.SysDictItemHashCache
import io.kudos.ms.sys.core.resource.cache.SysResourceHashCache
import io.kudos.ms.sys.core.resource.event.SysResourceBatchDeleted
import io.kudos.ms.sys.core.resource.event.SysResourceDeleted
import io.kudos.ms.sys.core.resource.event.SysResourceInserted
import io.kudos.ms.sys.core.resource.event.SysResourceUpdated
import io.kudos.ms.sys.core.system.cache.SysSystemHashCache
import io.kudos.ms.sys.core.resource.dao.SysResourceDao
import io.kudos.ms.sys.core.resource.model.po.SysResource
import io.kudos.ms.sys.core.resource.model.table.SysResources
import io.kudos.ms.sys.core.resource.service.iservice.ISysResourceService
import org.ktorm.dsl.isNull
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass


/**
 * Resource service.
 *
 * @author K
 * @since 1.0.0
 */
@Service
@Transactional
open class SysResourceService(
    dao: SysResourceDao,
    private val sysResourceHashCache: SysResourceHashCache,
    private val sysDictItemHashCache: SysDictItemHashCache,
    private val sysSystemHashCache: SysSystemHashCache,
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, SysResource, SysResourceDao>(dao), ISysResourceService {

    private val log = LogFactory.getLog(this::class)

    @Suppress("UNCHECKED_CAST")
    @Transactional(readOnly = true)
    override fun <R : Any> get(id: String, returnType: KClass<R>): R? =
        if (returnType == SysResourceCacheEntry::class) sysResourceHashCache.getResourceById(id) as R?
        else super.get(id, returnType)

    @Transactional(readOnly = true)
    override fun getResourceFromCache(id: String): SysResourceCacheEntry? = sysResourceHashCache.getResourceById(id)

    @Transactional(readOnly = true)
    override fun getResourceIdFromCacheBySubSystemAndUrl(subSystemCode: String, url: String): String? =
        sysResourceHashCache.getResourceBySubSystemCodeAndUrl(subSystemCode, url)?.id

    @Transactional(readOnly = true)
    override fun getResourceIdsFromCacheBySubSystemAndType(
        subSystemCode: String,
        resourceTypeDictCode: String
    ): List<String> = sysResourceHashCache.getResourcesBySubSystemCodeAndType(subSystemCode, resourceTypeDictCode)
        .map { it.id }

    @Transactional(readOnly = true)
    override fun getResourcesBySubSystemCode(subSystemCode: String): List<SysResourceRow> =
        dao.searchAs(Criteria(SysResource::subSystemCode eq subSystemCode))

    @Transactional(readOnly = true)
    override fun getChildResources(parentId: String): List<SysResourceRow> =
        dao.searchAs(Criteria(SysResource::parentId eq parentId))

    @Transactional(readOnly = true)
    override fun getResourceTree(subSystemCode: String, parentId: String?): List<SysResourceTreeRow> =
        buildResourceTree(getResourcesBySubSystemCode(subSystemCode), parentId)

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val resource = SysResource {
            this.id = id
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(resource),
            log = log,
            successMessage = "Updated resource id=$id active=$active.",
            failureMessage = "Failed to update resource id=$id active=$active!",
        ) {
            eventPublisher.publishEvent(SysResourceUpdated(id = id))
        }
    }

    @Transactional
    override fun moveResource(id: String, newParentId: String?, newOrderNum: Int?): Boolean {
        val resource = SysResource {
            this.id = id
            this.parentId = newParentId
            this.orderNum = newOrderNum
        }
        return completeCrudUpdate(
            success = dao.update(resource),
            log = log,
            successMessage = "Moved resource $id under parent=$newParentId, orderNum=$newOrderNum.",
            failureMessage = "Failed to move resource $id!",
        ) {
            eventPublisher.publishEvent(SysResourceUpdated(id = id))
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "Inserted resource id=$id.") {
            eventPublisher.publishEvent(SysResourceInserted(id = id))
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireStringId(any, "resource")
        val oldResource = dao.get(id)
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "Updated resource id=$id.",
            failureMessage = "Failed to update resource id=$id!",
        ) {
            syncResourceUpdate(any, id, oldResource)
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val resource = dao.get(id)
        if (resource == null) {
            log.warn("Resource id=$id no longer exists when attempting delete!")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "Deleted resource id=$id.",
            failureMessage = "Failed to delete resource id=$id!",
        ) {
            syncResourceDelete(resource)
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("Batch delete resources: expected ${ids.size}, actually deleted $count.")
        if (count > 0) {
            eventPublisher.publishEvent(SysResourceBatchDeleted(ids = ids))
        }
        return count
    }

    @Transactional(readOnly = true)
    override fun getResourcesFromCacheByIds(ids: Collection<String>): Map<String, SysResourceCacheEntry> =
        ids.takeIf { it.isNotEmpty() }
            ?.let { sysResourceHashCache.getResourcesByIds(it.toSet()) }
            ?: emptyMap()

    @Transactional(readOnly = true)
    override fun getResourcesFromCacheBySubSystemAndType(
        resourceType: ResourceTypeEnum,
        subSystemCode: String,
    ): List<SysResourceCacheEntry> = getCachedResourcesByType(subSystemCode, resourceType)

    @Transactional(readOnly = true)
    override fun getSimpleMenusFromCache(subSystemCode: String): List<BaseMenuTreeNode> {
        return buildMenuTree(getCachedResourcesByType(subSystemCode, ResourceTypeEnum.MENU)) { item ->
            BaseMenuTreeNode().apply {
                id = item.id
                title = item.name
                parentId = item.parentId
                seqNo = item.orderNum
            }
        }.sortedBy { it.seqNo }
    }

    @Transactional(readOnly = true)
    override fun getMenusFromCache(subSystemCode: String): List<MenuTreeNode> {
        return buildMenuTree(getCachedResourcesByType(subSystemCode, ResourceTypeEnum.MENU)) { item ->
            MenuTreeNode().apply {
                id = item.id
                title = item.name
                parentId = item.parentId
                seqNo = item.orderNum
                index = item.url
                icon = item.icon
            }
        }.sortedBy { it.seqNo }
    }

    @Transactional(readOnly = true)
    override fun getResourceIdFromCache(subSysDictCode: String, url: String): String? =
        getResourceIdFromCacheBySubSystemAndUrl(subSysDictCode, url)

    @Transactional(readOnly = true)
    override fun getDirectChildrenResourcesFromCache(
        resourceType: ResourceTypeEnum,
        parentId: String?,
        subSystemCode: String,
    ): List<SysResourceCacheEntry> = getCachedResourcesByType(subSystemCode, resourceType)
            .filter { it.parentId == parentId }

    @Transactional(readOnly = true)
    override fun getChildrenResourcesFromCache(
        subSystemCode: String,
        resourceType: ResourceTypeEnum,
        parentId: String
    ): List<SysResourceCacheEntry> {
        val resources = getCachedResourcesByType(subSystemCode, resourceType)
        val children = mutableListOf<SysResourceCacheEntry>()
        filterChildrenRecursively(parentId, children, resources)
        return children
    }

    /**
     * Recursively filter child resources.
     */
    private fun filterChildrenRecursively(
        parentId: String,
        children: MutableList<SysResourceCacheEntry>,
        resources: Collection<SysResourceCacheEntry>
    ) {
        val filteredChildren = resources.filter { it.parentId == parentId }
        children.addAll(filteredChildren)
        filteredChildren.forEach { filterChildrenRecursively(it.id, children, resources) }
    }

    /**
     * Assemble a flat resource list into a tree; returns the list of root nodes (nodes whose parentId is null/blank, or whose parent is not in the list).
     */
    private fun <T : BaseMenuTreeNode> buildMenuTree(
        resources: List<SysResourceCacheEntry>,
        nodeFactory: (SysResourceCacheEntry) -> T
    ): List<T> {
        val nodeMap = resources.associate { it.id to nodeFactory(it) }
        val roots = mutableListOf<T>()
        resources.sortedBy { it.orderNum ?: Int.MAX_VALUE }.forEach { item ->
            val node = nodeMap[item.id] as T
            if (item.parentId.isNullOrBlank()) {
                roots.add(node)
            } else {
                val parent = nodeMap[item.parentId] as? BaseMenuTreeNode
                if (parent != null) {
                    parent.children.add(node)
                } else {
                    roots.add(node)
                }
            }
        }
        return roots
    }

    @Transactional(readOnly = true)
    override fun loadDirectChildrenForTree(sysResourceQuery: SysResourceQuery): List<IdAndNameTreeNode<String>> {
        return when (if (sysResourceQuery.level == null) Int.MAX_VALUE else sysResourceQuery.level) {
            0 -> { // resource type
                val dictItems = sysDictItemHashCache.getDictItems(
                    SysConsts.ATOMIC_SERVICE_NAME, SysDictTypes.RESOURCE_TYPE
                )
                dictItems.map {
                    IdAndNameTreeNode(it.itemCode, it.itemName)
                }
            }

            1 -> { // sub-system
                val cacheEntries = sysSystemHashCache.getAllSystems()
                cacheEntries.map {
                    IdAndNameTreeNode(it.code, it.name)
                }
            }

            else -> { // resource
                val searchPayload = sysResourceQuery.copy(
                    active = sysResourceQuery.active.takeUnless { it == false }
                ).apply {
                    pageNo = null
                    pageSize = sysResourceQuery.pageSize
                    orders = sysResourceQuery.orders
                }
                @Suppress("UNCHECKED_CAST")
                dao.search(
                    searchPayload,
                    whereConditionFactory = { column, _ ->
                        if (column.name == SysResources.parentId.name && searchPayload.level == 2) { // level 1 = resource type, level 2 = sub-system, SysResource starts from level 3
                            column.isNull()
                        } else null
                    },
                    returnItemClassOverride = IdAndNameTreeNode::class
                ) as List<IdAndNameTreeNode<String>>
            }
        }
    }

    @Transactional(readOnly = true)
    override fun fetchAllParentIds(id: String): List<String> {
        val results = mutableListOf<String>()
        collectParentIds(id, results)
        results.reverse()
        return results
    }

    @Transactional(readOnly = true)
    override fun getDetailWithOptionalParents(
        id: String,
        includeParents: Boolean,
    ): SysResourceDetail? {
        val detail = get(id, SysResourceDetail::class) ?: return null
        if (includeParents) {
            detail.parentIds = fetchAllParentIds(id)
        }
        return detail
    }

    /**
     * Fetch from the resource hash cache by sub-system code + resource type.
     * Shared by multiple `*FromCache` overrides to avoid repeating the "enum → code" conversion at every call site.
     *
     * @param subSystemCode sub-system code
     * @param resourceType resource type enum
     * @return list of resource cache entries
     * @author K
     * @since 1.0.0
     */
    private fun getCachedResourcesByType(
        subSystemCode: String,
        resourceType: ResourceTypeEnum,
    ): List<SysResourceCacheEntry> =
        sysResourceHashCache.getResourcesBySubSystemCodeAndType(subSystemCode, resourceType.code)

    /**
     * Assemble a flat [SysResourceRow] list into a tree by parentId.
     *
     * Algorithm: map each row to [SysResourceTreeRow] and index by id, then walk and attach each node under its parent's children;
     * nodes whose parent is not in the list are treated as roots. Finally sort recursively by orderNum.
     *
     * @param records flat resource list to assemble
     * @param parentId optional root selector: when non-null, returns only that node's subtree
     * @return list of root nodes (or children of the specified parentId)
     * @author K
     * @since 1.0.0
     */
    private fun buildResourceTree(
        records: List<SysResourceRow>,
        parentId: String?,
    ): List<SysResourceTreeRow> {
        val nodeMap = records.map(::toTreeRow).associateBy { it.id }
        val rootNodes = mutableListOf<SysResourceTreeRow>()

        nodeMap.values.forEach { node ->
            node.parentId?.let(nodeMap::get)?.children?.add(node) ?: rootNodes.add(node)
        }
        sortResourceTree(rootNodes)
        return parentId?.let { nodeMap[it]?.children.orEmpty() } ?: rootNodes
    }

    /**
     * Shallow-copy a [SysResourceRow] into a [SysResourceTreeRow]; initial children is an empty container ready for attachment.
     *
     * @param record single resource record
     * @return tree node
     * @author K
     * @since 1.0.0
     */
    private fun toTreeRow(record: SysResourceRow): SysResourceTreeRow =
        SysResourceTreeRow(
            id = record.id,
            name = record.name,
            url = record.url,
            resourceTypeDictCode = record.resourceTypeDictCode,
            parentId = record.parentId,
            orderNum = record.orderNum,
            icon = record.icon,
            subSystemCode = record.subSystemCode,
            remark = record.remark,
            active = record.active,
            builtIn = record.builtIn,
            children = mutableListOf(),
        )

    /**
     * Recursively sort the node list and its subtrees by ascending orderNum; null orderNum sorts last (treated as Int.MAX_VALUE).
     *
     * @param nodes node list to sort (in-place)
     * @author K
     * @since 1.0.0
     */
    private fun sortResourceTree(nodes: MutableList<SysResourceTreeRow>) {
        nodes.sortBy { it.orderNum ?: Int.MAX_VALUE }
        nodes.forEach { node ->
            node.children?.let(::sortResourceTree)
        }
    }

    /**
     * Post-update sync: currently publishes a [SysResourceUpdated] event; cache subscribers refresh themselves.
     *
     * `any` / `oldResource` are not currently used — they remain as parameters to leave room for future "differential cache invalidation"
     * (e.g. when parentId changes we may need to invalidate both old and new parents' subtree caches).
     *
     * @param any update payload (with new field values)
     * @param id resource id
     * @param oldResource snapshot of the resource before update, may be null
     * @author K
     * @since 1.0.0
     */
    private fun syncResourceUpdate(any: Any, id: String, oldResource: SysResource?) {
        eventPublisher.publishEvent(SysResourceUpdated(id = id))
    }

    /**
     * Post-delete sync: publishes a [SysResourceDeleted] event so subscribers can clean up caches / external references.
     *
     * @param resource the deleted resource object (with id)
     * @author K
     * @since 1.0.0
     */
    private fun syncResourceDelete(resource: SysResource) {
        eventPublisher.publishEvent(SysResourceDeleted(id = resource.id))
    }

    /**
     * Walk bottom-up along the parentId chain to collect all ancestor ids; stops when a node is missing or its parentId is blank.
     *
     * Note: the caller [fetchAllParentIds] then `reverse()`s the result, so this method yields "nearest to furthest".
     *
     * @param itemId starting node id
     * @param results accumulator (appended in place)
     * @author K
     * @since 1.0.0
     */
    private fun collectParentIds(itemId: String, results: MutableList<String>) {
        var currentId: String? = itemId
        while (currentId != null) {
            val parentId = sysResourceHashCache.getResourceById(currentId)?.parentId ?: break
            results += parentId
            currentId = parentId
        }
    }

}
