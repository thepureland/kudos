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
            successMessage = "更新id为${id}的资源的启用状态为${active}。",
            failureMessage = "更新id为${id}的资源的启用状态为${active}失败！",
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
            successMessage = "移动资源${id}到父节点${newParentId}，排序号${newOrderNum}。",
            failureMessage = "移动资源${id}失败！",
        ) {
            eventPublisher.publishEvent(SysResourceUpdated(id = id))
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "新增id为${id}的资源。") {
            eventPublisher.publishEvent(SysResourceInserted(id = id))
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireStringId(any, "资源")
        val oldResource = dao.get(id)
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "更新id为${id}的资源。",
            failureMessage = "更新id为${id}的资源失败！",
        ) {
            syncResourceUpdate(any, id, oldResource)
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
            syncResourceDelete(resource)
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除资源，期望删除${ids.size}条，实际删除${count}条。")
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

    @Transactional(readOnly = true)
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
     * 走资源 hash 缓存按子系统编码 + 资源类型取列表。
     * 给多个 `*FromCache` overrides 复用，避免在每处重复"枚举 → code"的转换。
     *
     * @param subSystemCode 子系统编码
     * @param resourceType 资源类型枚举
     * @return 资源缓存条目列表
     * @author K
     * @since 1.0.0
     */
    private fun getCachedResourcesByType(
        subSystemCode: String,
        resourceType: ResourceTypeEnum,
    ): List<SysResourceCacheEntry> =
        sysResourceHashCache.getResourcesBySubSystemCodeAndType(subSystemCode, resourceType.code)

    /**
     * 把扁平的 [SysResourceRow] 列表按 parentId 装配成树。
     *
     * 算法：先把每行映射为 [SysResourceTreeRow] 并按 id 做 map → 再遍历挂到父节点的 children；
     * 父节点不在列表里则视为根。最后按 orderNum 递归排序。
     *
     * @param records 待装配的扁平资源列表
     * @param parentId 指定根：非空时只返回该节点的子树
     * @return 根节点列表（或指定 parentId 的子节点）
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
     * 把 [SysResourceRow] 浅拷贝成 [SysResourceTreeRow]，初始 children 为空容器以备后续挂载。
     *
     * @param record 单条资源记录
     * @return 树节点
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
     * 递归按 orderNum 升序排序节点列表及其子树；null orderNum 排到最末（按 Int.MAX_VALUE 处理）。
     *
     * @param nodes 待排序节点列表（in-place 排序）
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
     * 资源更新后的同步动作：当前只发 [SysResourceUpdated] 事件，由各级缓存订阅者自行刷新。
     *
     * 入参 `any` / `oldResource` 暂未使用——保留参数是为给后续做"差量缓存清理"留扩展点
     * （例如旧 parentId 变化时需要同时清新旧父节点的子树缓存）。
     *
     * @param any 更新入参（含新字段值）
     * @param id 资源 id
     * @param oldResource 更新前的资源快照，可为 null
     * @author K
     * @since 1.0.0
     */
    private fun syncResourceUpdate(any: Any, id: String, oldResource: SysResource?) {
        eventPublisher.publishEvent(SysResourceUpdated(id = id))
    }

    /**
     * 资源删除后的同步动作：发 [SysResourceDeleted] 事件由订阅者清理缓存/外链。
     *
     * @param resource 已被删除的资源对象（带 id）
     * @author K
     * @since 1.0.0
     */
    private fun syncResourceDelete(resource: SysResource) {
        eventPublisher.publishEvent(SysResourceDeleted(id = resource.id))
    }

    /**
     * 自底向上沿 parentId 链收集所有祖先 id；链上某节点不存在或 parentId 为空即终止。
     *
     * 注意：调用方 [fetchAllParentIds] 会再 `reverse()`，所以结果是"从近到远"。
     *
     * @param itemId 起点节点 id
     * @param results 累积容器（追加）
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
