package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.ms.sys.common.consts.SysConsts
import io.kudos.ms.sys.common.consts.SysDictTypes
import io.kudos.ms.sys.common.enums.resource.ResourceTypeEnum
import io.kudos.ms.sys.common.vo.resource.SysResourceCacheEntry
import io.kudos.ms.sys.common.vo.resource.request.SysResourceQuery
import io.kudos.ms.sys.common.vo.resource.response.BaseMenuTreeNode
import io.kudos.ms.sys.common.vo.resource.response.MenuTreeNode
import io.kudos.ms.sys.common.vo.resource.response.SysResourceRow
import io.kudos.ms.sys.common.vo.resource.response.SysResourceTreeRow
import io.kudos.ms.sys.core.cache.SysDictItemHashCache
import io.kudos.ms.sys.core.cache.SysResourceHashCache
import io.kudos.ms.sys.core.cache.SysSystemHashCache
import io.kudos.ms.sys.core.dao.SysResourceDao
import io.kudos.ms.sys.core.model.po.SysResource
import io.kudos.ms.sys.core.model.table.SysResources
import io.kudos.ms.sys.core.service.iservice.ISysResourceService
import org.ktorm.dsl.isNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass


/**
 * 资源业务
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
) : BaseCrudService<String, SysResource, SysResourceDao>(dao), ISysResourceService {

    private val log = LogFactory.getLog(this::class)

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        return if (returnType == SysResourceCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            sysResourceHashCache.getResourceById(id) as R?
        } else {
            super.get(id, returnType)
        }
    }

    override fun getResourceFromCache(id: String): SysResourceCacheEntry? = sysResourceHashCache.getResourceById(id)

    override fun getResourceIdFromCacheBySubSystemAndUrl(subSystemCode: String, url: String): String? {
        return sysResourceHashCache.getResourceBySubSystemCodeAndUrl(subSystemCode, url)?.id
    }

    override fun getResourceIdsFromCacheBySubSystemAndType(
        subSystemCode: String,
        resourceTypeDictCode: String
    ): List<String> {
        return sysResourceHashCache.getResourcesBySubSystemCodeAndType(subSystemCode, resourceTypeDictCode)
            .map { it.id }
    }

    override fun getResourcesBySubSystemCode(subSystemCode: String): List<SysResourceRow> =
        dao.searchAs(Criteria(SysResource::subSystemCode eq subSystemCode))

    override fun getChildResources(parentId: String): List<SysResourceRow> =
        dao.searchAs(Criteria(SysResource::parentId eq parentId))

    override fun getResourceTree(subSystemCode: String, parentId: String?): List<SysResourceTreeRow> {
        val treeNodes = getResourcesBySubSystemCode(subSystemCode).map { record ->
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
        }
        val nodeMap = treeNodes.associateBy { it.id }
        val rootNodes = mutableListOf<SysResourceTreeRow>()

        treeNodes.forEach { node ->
            val parent = node.parentId?.let(nodeMap::get)
            if (parent == null) {
                rootNodes += node
            } else {
                parent.children?.add(node)
            }
        }

        fun sortTree(nodes: MutableList<SysResourceTreeRow>) {
            nodes.sortBy { it.orderNum ?: Int.MAX_VALUE }
            nodes.forEach { node ->
                node.children?.let(::sortTree)
            }
        }

        sortTree(rootNodes)
        return parentId?.let { nodeMap[it]?.children.orEmpty() } ?: rootNodes
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val resource = SysResource {
            this.id = id
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(resource),
            log = log,
            successMessage = "更新id为${id}的资源的启用状态为${active}。",
            failureMessage = "更新id为${id}的资源的启用状态为${active}失败！",
        ) {
            sysResourceHashCache.syncOnUpdateActive(id, active)
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
            successMessage = "移动资源${id}到父节点${newParentId}，排序号${newOrderNum}。",
            failureMessage = "移动资源${id}失败！",
        ) {
            sysResourceHashCache.syncOnUpdate(id)
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "新增id为${id}的资源。") {
            sysResourceHashCache.syncOnInsert(any, id)
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireResourceId(any)
        val oldResource = dao.get(id)
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "更新id为${id}的资源。",
            failureMessage = "更新id为${id}的资源失败！",
        ) {
            sysResourceHashCache.syncOnUpdate(id)
            val oldUrl = oldResource?.url
            sysResourceHashCache.syncOnUpdate(any, id, oldUrl)
            val oldSubSystemCode = oldResource?.subSystemCode
            val oldResourceTypeDictCode = oldResource?.resourceTypeDictCode
            if (oldSubSystemCode != null && oldResourceTypeDictCode != null) {
                sysResourceHashCache.syncOnUpdate(any, id, oldSubSystemCode, oldResourceTypeDictCode)
            }
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val resource = dao.get(id)
        if (resource == null) {
            log.warn("删除id为${id}的资源时，发现其已不存在！")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "删除id为${id}的资源。",
            failureMessage = "删除id为${id}的资源失败！",
        ) {
            sysResourceHashCache.syncOnDelete(id, resource.subSystemCode, resource.url)
            sysResourceHashCache.syncOnDelete(id, resource.subSystemCode, resource.resourceTypeDictCode)
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        @Suppress("UNCHECKED_CAST")
        val resources = dao.inSearchById(ids)
        val count = super.batchDelete(ids)
        log.debug("批量删除资源，期望删除${ids.size}条，实际删除${count}条。")
        sysResourceHashCache.syncOnBatchDelete(ids)
        resources.forEach { resource ->
            sysResourceHashCache.syncOnDelete(resource.id, resource.subSystemCode, resource.url)
            sysResourceHashCache.syncOnDelete(resource.id, resource.subSystemCode, resource.resourceTypeDictCode)
        }
        return count
    }

    override fun getResourcesFromCacheByIds(ids: Collection<String>): Map<String, SysResourceCacheEntry> =
        ids.takeIf { it.isNotEmpty() }
            ?.let { sysResourceHashCache.getResourcesByIds(it.toSet()) }
            ?: emptyMap()

    override fun getResourcesFromCacheBySubSystemAndType(
        resourceType: ResourceTypeEnum,
        subSystemCode: String,
    ): List<SysResourceCacheEntry> = sysResourceHashCache.getResourcesBySubSystemCodeAndType(subSystemCode, resourceType.code)

    override fun getSimpleMenusFromCache(subSystemCode: String): List<BaseMenuTreeNode> {
        val resources =
            sysResourceHashCache.getResourcesBySubSystemCodeAndType(subSystemCode, ResourceTypeEnum.MENU.code)
        return buildMenuTree(resources) { item ->
            BaseMenuTreeNode().apply {
                id = item.id
                title = item.name
                parentId = item.parentId
                seqNo = item.orderNum
            }
        }.sortedBy { it.seqNo }
    }

    override fun getMenusFromCache(subSystemCode: String): List<MenuTreeNode> {
        val resources =
            sysResourceHashCache.getResourcesBySubSystemCodeAndType(subSystemCode, ResourceTypeEnum.MENU.code)
        return buildMenuTree(resources) { item ->
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

    override fun getResourceIdFromCache(subSysDictCode: String, url: String): String? =
        getResourceIdFromCacheBySubSystemAndUrl(subSysDictCode, url)

    override fun getDirectChildrenResourcesFromCache(
        resourceType: ResourceTypeEnum,
        parentId: String?,
        subSystemCode: String,
    ): List<SysResourceCacheEntry> =
        sysResourceHashCache.getResourcesBySubSystemCodeAndType(subSystemCode, resourceType.code)
            .filter { it.parentId == parentId }

    override fun getChildrenResourcesFromCache(
        subSystemCode: String,
        resourceType: ResourceTypeEnum,
        parentId: String
    ): List<SysResourceCacheEntry> {
        val resources = sysResourceHashCache.getResourcesBySubSystemCodeAndType(subSystemCode, resourceType.code)
        val children = mutableListOf<SysResourceCacheEntry>()
        filterChildrenRecursively(parentId, children, resources)
        return children
    }

    /**
     * 递归地过滤孩子资源
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
     * 将资源列表组装为树，返回根节点列表（parentId 为 null 或空的节点，或父节点不在列表中的节点）。
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

    override fun loadDirectChildrenForTree(sysResourceQuery: SysResourceQuery): List<IdAndNameTreeNode<String>> {
        return when (if (sysResourceQuery.level == null) Int.MAX_VALUE else sysResourceQuery.level) {
            0 -> { // 资源类型
                val dictItems = sysDictItemHashCache.getDictItems(
                    SysConsts.ATOMIC_SERVICE_NAME, SysDictTypes.RESOURCE_TYPE
                )
                dictItems.map {
                    IdAndNameTreeNode(it.itemCode, it.itemName)
                }
            }

            1 -> { // 子系统
                val cacheEntries = sysSystemHashCache.getAllSystems()
                cacheEntries.map {
                    IdAndNameTreeNode(it.code, it.name)
                }
            }

            else -> { // 资源
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
                        if (column.name == SysResources.parentId.name && searchPayload.level == 2) { // 1层是资源类型，2层是子系统，从第3层开始才是SysResource
                            column.isNull()
                        } else null
                    },
                    returnItemClassOverride = IdAndNameTreeNode::class
                ) as List<IdAndNameTreeNode<String>>
            }
        }
    }

    override fun fetchAllParentIds(id: String): List<String> {
        val results = mutableListOf<String>()
        recursionFindAllParentId(id, results)
        results.reverse()
        return results
    }

    private fun recursionFindAllParentId(itemId: String, results: MutableList<String>) {
        val parentId = sysResourceHashCache.getResourceById(itemId)?.parentId ?: return
        results.add(parentId)
        recursionFindAllParentId(parentId, results)
    }

    private fun requireResourceId(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新资源时不支持的入参类型: ${any::class.qualifiedName}")
}
