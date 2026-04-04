package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.base.tree.ListToTreeConverter
import io.kudos.ms.sys.common.vo.system.SysSystemCacheEntry
import io.kudos.ms.sys.core.cache.SysSystemHashCache
import io.kudos.ms.sys.core.dao.SysSystemDao
import io.kudos.ms.sys.core.model.po.SysSystem
import io.kudos.ms.sys.core.service.iservice.ISysSystemService
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
    dao: SysSystemDao,
    private val sysSystemHashCache: SysSystemHashCache,
) : BaseCrudService<String, SysSystem, SysSystemDao>(dao), ISysSystemService {

    private val log = LogFactory.getLog(this::class)

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        return if (returnType == SysSystemCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            sysSystemHashCache.getSystemByCode(id) as R?
        } else {
            super.get(id, returnType)
        }
    }

    override fun getSystemFromCache(code: String): SysSystemCacheEntry? = sysSystemHashCache.getSystemByCode(code)

    override fun getFullSystemTree(): List<IdAndNameTreeNode<String>> =
        ListToTreeConverter.convert(getAllSystemsFromCache().map { IdAndNameTreeNode(it.code, it.name, it.parentCode) })

    override fun getAllSystemsFromCache(): List<SysSystemCacheEntry> = sysSystemHashCache.getAllSystems()

    override fun getSystemsExcludeSubSystemFromCache(): List<SysSystemCacheEntry> = sysSystemHashCache.getSystemsByType(false)

    @Transactional
    override fun updateActive(code: String, active: Boolean): Boolean {
        val system = SysSystem {
            this.code = code
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(system),
            log = log,
            successMessage = "更新编码为${code}的系统的启用状态为${active}。",
            failureMessage = "更新编码为${code}的系统的启用状态为${active}失败！",
        ) {
            sysSystemHashCache.syncOnUpdate(system, code)
        }
    }

    override fun getSubSystemsFromCache(systemCode: String): List<SysSystemCacheEntry> {
        val subSystems = sysSystemHashCache.getAllSystems()
            .filter { it.parentCode == systemCode }
        if (subSystems.isNotEmpty()) {
            return subSystems
        }

        sysSystemHashCache.reloadAll(clear = false)
        return sysSystemHashCache.getAllSystems().filter { it.parentCode == systemCode }
    }

    @Transactional
    override fun insert(any: Any): String {
        val code = super.insert(any)
        completeCrudInsert(log, "新增编码为${code}的系统。") {
            sysSystemHashCache.syncOnInsert(any, code)
        }
        return code
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val code = requireSystemCode(any)
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "更新编码为${code}的系统。",
            failureMessage = "更新编码为${code}的系统失败！",
        ) {
            sysSystemHashCache.syncOnUpdate(any, code)
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val sysSystem = dao.get(id)
        if (sysSystem == null) {
            log.warn("删除编码为${id}的系统时，发现其已不存在！")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "删除编码为${id}的系统成功！",
            failureMessage = "删除编码为${id}的系统失败！",
        ) {
            sysSystemHashCache.syncOnDelete(id)
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除系统，期望删除${ids.size}条，实际删除${count}条。")
        sysSystemHashCache.syncOnBatchDelete(ids)
        return count
    }

    private fun requireSystemCode(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新系统时不支持的入参类型: ${any::class.qualifiedName}")
}
