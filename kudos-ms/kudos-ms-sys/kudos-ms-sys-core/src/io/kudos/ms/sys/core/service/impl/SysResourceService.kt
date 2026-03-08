package io.kudos.ms.sys.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.enums.ResourceTypeEnum
import io.kudos.ms.sys.common.vo.resource.*
import io.kudos.ms.sys.core.cache.SysResourceHashCache
import io.kudos.ms.sys.core.dao.SysResourceDao
import io.kudos.ms.sys.core.model.po.SysResource
import io.kudos.ms.sys.core.service.iservice.ISysResourceService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 资源业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysResourceService : BaseCrudService<String, SysResource, SysResourceDao>(), ISysResourceService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    @Resource
    private lateinit var sysResourceHashCache: SysResourceHashCache

    override fun getResourceById(id: String): SysResourceCacheItem? {
        return sysResourceHashCache.getResourceById(id)
    }

    override fun getResourceBySubSystemAndUrl(subSystemCode: String, url: String): String? {
        return sysResourceHashCache.getResourceBySubSystemCodeAndUrl(subSystemCode, url)?.id
    }

    override fun getResourceIdsBySubSystemAndType(subSystemCode: String, resourceTypeDictCode: String): List<String> {
        return sysResourceHashCache.getResourcesBySubSystemCodeAndType(subSystemCode, resourceTypeDictCode)
            .map { it.id }
    }

    override fun getResourcesBySubSystemCode(subSystemCode: String): List<SysResourceRecord> {
        val criteria = Criteria(SysResource::subSystemCode eq subSystemCode)
        return dao.searchAs<SysResourceRecord>(criteria)
    }

    override fun getChildResources(parentId: String): List<SysResourceRecord> {
        val criteria = Criteria(SysResource::parentId eq parentId)
        return dao.searchAs<SysResourceRecord>(criteria)
    }

    override fun getResourceTree(subSystemCode: String, parentId: String?): List<SysResourceTreeRecord> {
        val criteria = Criteria.and(
            SysResource::subSystemCode eq subSystemCode,
            SysResource::parentId eq parentId
        )
        val records = dao.searchAs<SysResourceRecord>(criteria)

        // 转换为树节点
        val treeNodes = records.map { record ->
            SysResourceTreeRecord(children = mutableListOf()).apply {
                BeanKit.copyProperties(record, this)
            }
        }

        // 构建树形结构
        val nodeMap = treeNodes.associateBy { it.id }
        val rootNodes = mutableListOf<SysResourceTreeRecord>()

        treeNodes.forEach { node ->
            if (node.parentId == null) {
                rootNodes.add(node)
            } else {
                val parent = nodeMap[node.parentId]
                parent?.children?.add(node)
            }
        }

        // 按 orderNum 排序
        fun sortTree(nodes: List<SysResourceTreeRecord>) {
            nodes.sortedBy { it.orderNum ?: Int.MAX_VALUE }.forEach { node ->
                node.children?.let { sortTree(it) }
            }
        }
        sortTree(rootNodes)

        return rootNodes.sortedBy { it.orderNum ?: Int.MAX_VALUE }
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val resource = SysResource {
            this.id = id
            this.active = active
        }
        val success = dao.update(resource)
        if (success) {
            log.debug("更新id为${id}的资源的启用状态为${active}。")
            sysResourceHashCache.syncOnUpdateActive(id, active)
        } else {
            log.error("更新id为${id}的资源的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun moveResource(id: String, newParentId: String?, newOrderNum: Int?): Boolean {
        val resource = SysResource {
            this.id = id
            this.parentId = newParentId
            this.orderNum = newOrderNum
        }
        val success = dao.update(resource)
        if (success) {
            log.debug("移动资源${id}到父节点${newParentId}，排序号${newOrderNum}。")
            sysResourceHashCache.syncOnUpdate(id)
        } else {
            log.error("移动资源${id}失败！")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的资源。")
        sysResourceHashCache.syncOnInsert(id)
        sysResourceHashCache.syncOnInsert(any, id)
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = BeanKit.getProperty(any, SysResource::id.name) as String
        val oldResource = dao.get(id)
        val success = super.update(any)
        if (success) {
            log.debug("更新id为${id}的资源。")
            sysResourceHashCache.syncOnUpdate(id)
            val oldUrl = oldResource?.url
            sysResourceHashCache.syncOnUpdate(any, id, oldUrl)
            val oldSubSystemCode = oldResource?.subSystemCode
            val oldResourceTypeDictCode = oldResource?.resourceTypeDictCode
            if (oldSubSystemCode != null && oldResourceTypeDictCode != null) {
                sysResourceHashCache.syncOnUpdate(any, id, oldSubSystemCode, oldResourceTypeDictCode)
            }
        } else {
            log.error("更新id为${id}的资源失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val resource = dao.get(id)
        if (resource == null) {
            log.warn("删除id为${id}的资源时，发现其已不存在！")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的资源。")
            sysResourceHashCache.syncOnDelete(id, resource.subSystemCode, resource.url)
            sysResourceHashCache.syncOnDelete(id, resource.subSystemCode, resource.resourceTypeDictCode)
        } else {
            log.error("删除id为${id}的资源失败！")
        }
        return success
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

    override fun getResource(resourceId: String): SysResourceCacheItem? {
        return sysResourceHashCache.getResourceById(resourceId)
    }

    override fun getResources(resourceIds: Collection<String>): Map<String, SysResourceCacheItem> {
        if (resourceIds.isEmpty()) return emptyMap()
        return sysResourceHashCache.getResourcesByIds(resourceIds.toSet())
    }

    override fun getResources(
        resourceType: ResourceTypeEnum,
        subSystemCode: String,
    ): List<SysResourceCacheItem> {
        return sysResourceHashCache.getResourcesBySubSystemCodeAndType(subSystemCode, resourceType.code)
    }

    override fun getSimpleMenus(subSystemCode: String): List<BaseMenuTreeNode> {
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

    override fun getMenus(subSystemCode: String): List<MenuTreeNode> {
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

    override fun getResourceId(subSysDictCode: String, url: String): String? {
        return sysResourceHashCache.getResourceBySubSystemCodeAndUrl(subSysDictCode, url)?.id
    }

    override fun getDirectChildrenResources(
        resourceType: ResourceTypeEnum,
        parentId: String?,
        subSystemCode: String,
    ): List<SysResourceCacheItem> {
        val list = sysResourceHashCache.getResourcesBySubSystemCodeAndType(subSystemCode, resourceType.code)
        return list.filter { it.parentId == parentId }
    }

    override fun getChildrenResources(
        subSystemCode: String,
        resourceType: ResourceTypeEnum,
        parentId: String
    ): List<SysResourceCacheItem> {
        val resources = sysResourceHashCache.getResourcesBySubSystemCodeAndType(subSystemCode, resourceType.code)
        val children = mutableListOf<SysResourceCacheItem>()
        filterChildrenRecursively(parentId, children, resources)
        return children
    }

    /**
     * 递归地过滤孩子资源
     */
    private fun filterChildrenRecursively(
        parentId: String,
        children: MutableList<SysResourceCacheItem>,
        resources: Collection<SysResourceCacheItem>
    ) {
        val filteredChildren = resources.filter { it.parentId == parentId }
        children.addAll(filteredChildren)
        filteredChildren.forEach { filterChildrenRecursively(it.parentId!!, children, resources) }
    }

    /**
     * 将资源列表组装为树，返回根节点列表（parentId 为 null 或空的节点，或父节点不在列表中的节点）。
     */
    private fun <T : BaseMenuTreeNode> buildMenuTree(
        resources: List<SysResourceCacheItem>,
        nodeFactory: (SysResourceCacheItem) -> T
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

    //endregion your codes 2

}