package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.base.tree.ListToTreeConverter
import io.kudos.ms.sys.common.vo.system.SysSystemCacheEntry
import io.kudos.ms.sys.core.cache.SysSystemHashCache
import io.kudos.ms.sys.core.dao.SysSystemDao
import io.kudos.ms.sys.core.model.po.SysSystem
import io.kudos.ms.sys.core.service.iservice.ISysSystemService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass


/**
 * 系统业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class SysSystemService(
    dao: SysSystemDao
) : BaseCrudService<String, SysSystem, SysSystemDao>(dao), ISysSystemService {

    private val log = LogFactory.getLog(this::class)

    @Autowired
    private lateinit var sysSystemHashCache: SysSystemHashCache

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        return if (returnType == SysSystemCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            sysSystemHashCache.getSystemByCode(id) as R?
        } else {
            super.get(id, returnType)
        }
    }

    override fun getSystemFromCache(code: String): SysSystemCacheEntry? {
        return sysSystemHashCache.getSystemByCode(code)
    }

    override fun getFullSystemTree(): List<IdAndNameTreeNode<String>> {
        val cacheEntries = getAllSystemsFromCache()
        val nodes = cacheEntries.map { IdAndNameTreeNode(it.code, it.name, it.parentCode) }
        return ListToTreeConverter.convert(nodes)
    }

    override fun getAllSystemsFromCache(): List<SysSystemCacheEntry> {
        return sysSystemHashCache.getAllSystems()
    }

    override fun getSystemsExcludeSubSystemFromCache(): List<SysSystemCacheEntry> {
        return sysSystemHashCache.getSystemsByType(false)
    }

    @Transactional
    override fun updateActive(code: String, active: Boolean): Boolean {
        val system = SysSystem {
            this.code = code
            this.active = active
        }
        val success = dao.update(system)
        if (success) {
            log.debug("更新编码为${code}的系统的启用状态为${active}。")
            sysSystemHashCache.syncOnUpdate(system, code)
        } else {
            log.error("更新编码为${code}的系统的启用状态为${active}失败！")
        }
        return success
    }

    override fun getSubSystemsFromCache(systemCode: String): List<SysSystemCacheEntry> {
        return sysSystemHashCache.getAllSystems()
            .filter { it.parentCode == systemCode }
    }

    @Transactional
    override fun insert(any: Any): String {
        val code = super.insert(any)
        log.debug("新增编码为${code}的系统。")
        sysSystemHashCache.syncOnInsert(any, code) // 同步缓存
        return code
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val code = BeanKit.getProperty(any, SysSystem::code.name) as String
        if (success) {
            log.debug("更新编码为${code}的系统。")
            sysSystemHashCache.syncOnUpdate(any, code)
        } else {
            log.error("更新编码为${code}的系统失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val sysSystem = dao.get(id)
        if (sysSystem == null) {
            log.warn("删除编码为${id}的系统时，发现其已不存在！")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除编码为${id}的系统成功！")
            sysSystemHashCache.syncOnDelete(id)
        } else {
            log.error("删除编码为${id}的系统失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除系统，期望删除${ids.size}条，实际删除${count}条。")
        sysSystemHashCache.syncOnBatchDelete(ids)
        return count
    }


}
