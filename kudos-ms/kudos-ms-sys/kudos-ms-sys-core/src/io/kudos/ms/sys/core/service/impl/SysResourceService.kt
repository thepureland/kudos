package io.kudos.ms.sys.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.ms.sys.common.vo.resource.SysResourceRecord
import io.kudos.ms.sys.common.vo.resource.SysResourceSearchPayload
import io.kudos.ms.sys.common.vo.resource.SysResourceTreeRecord
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
        return sysResourceHashCache.getResourcesBySubSystemCodeAndType(subSystemCode, resourceTypeDictCode).map { it.id!! }
    }

    override fun getResourcesBySubSystemCode(subSystemCode: String): List<SysResourceRecord> {
        val searchPayload = SysResourceSearchPayload().apply {
            this.subSystemCode = subSystemCode
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload) as List<SysResourceRecord>
    }

    override fun getChildResources(parentId: String): List<SysResourceRecord> {
        val searchPayload = SysResourceSearchPayload().apply {
            this.parentId = parentId
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload) as List<SysResourceRecord>
    }

    override fun getResourceTree(subSystemCode: String, parentId: String?): List<SysResourceTreeRecord> {
        val searchPayload = SysResourceSearchPayload().apply {
            this.subSystemCode = subSystemCode
            this.parentId = parentId
        }
        @Suppress("UNCHECKED_CAST")
        val records = dao.search(searchPayload) as List<SysResourceRecord>
        
        // 转换为树节点
        val treeNodes = records.map { record ->
            SysResourceTreeRecord().apply {
                BeanKit.copyProperties(record, this)
                this.children = mutableListOf()
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
            sysResourceHashCache.syncOnUpdate(id)
            sysResourceHashCache.syncOnUpdateActive(id, active)
            sysResourceHashCache.syncOnUpdateActive(id)
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
            sysResourceHashCache.syncOnDelete(resource.id!!, resource.subSystemCode, resource.url)
            sysResourceHashCache.syncOnDelete(resource.id!!, resource.subSystemCode, resource.resourceTypeDictCode)
        }
        return count
    }

    //endregion your codes 2

}