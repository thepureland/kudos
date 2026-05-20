package io.kudos.ms.sys.core.system.service.impl

import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.base.tree.ListToTreeConverter
import io.kudos.ms.sys.common.system.vo.SysSystemCacheEntry
import io.kudos.ms.sys.core.system.cache.SysSystemHashCache
import io.kudos.ms.sys.core.system.dao.SysSystemDao
import io.kudos.ms.sys.core.system.event.SysSystemBatchDeleted
import io.kudos.ms.sys.core.system.event.SysSystemDeleted
import io.kudos.ms.sys.core.system.event.SysSystemInserted
import io.kudos.ms.sys.core.system.event.SysSystemUpdated
import io.kudos.ms.sys.core.system.model.po.SysSystem
import io.kudos.ms.sys.core.system.service.iservice.ISysSystemService
import org.springframework.context.ApplicationEventPublisher
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
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, SysSystem, SysSystemDao>(dao), ISysSystemService {

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        return if (returnType == SysSystemCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            sysSystemHashCache.getSystemByCode(id) as R?
        } else {
            super.get(id, returnType)
        }
    }

    @Transactional(readOnly = true)
    override fun getSystemFromCache(code: String): SysSystemCacheEntry? = sysSystemHashCache.getSystemByCode(code)

    @Transactional(readOnly = true)
    override fun getFullSystemTree(): List<IdAndNameTreeNode<String>> =
        ListToTreeConverter.convert(getAllSystemsFromCache().map { IdAndNameTreeNode(it.code, it.name, it.parentCode) })

    @Transactional(readOnly = true)
    override fun getAllSystemsFromCache(): List<SysSystemCacheEntry> = sysSystemHashCache.getAllSystems()

    @Transactional(readOnly = true)
    override fun getSystemsExcludeSubSystemFromCache(): List<SysSystemCacheEntry> = sysSystemHashCache.getSystemsByType(false)

    @Transactional(readOnly = true)
    override fun getActiveSubSystemCodes(): List<String> =
        getAllSystemsFromCache().filter { it.subSystem && it.active }.map { it.code }

    @Transactional(readOnly = true)
    override fun getActiveSystemCodes(): List<String> =
        getSystemsExcludeSubSystemFromCache().filter { it.active }.map { it.code }

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
            eventPublisher.publishEvent(SysSystemUpdated(id = code))
        }
    }

    @Transactional(readOnly = true)
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
            eventPublisher.publishEvent(SysSystemInserted(id = code))
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
            eventPublisher.publishEvent(SysSystemUpdated(id = code))
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
            eventPublisher.publishEvent(SysSystemDeleted(id = id))
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除系统，期望删除${ids.size}条，实际删除${count}条。")
        if (count > 0) {
            eventPublisher.publishEvent(SysSystemBatchDeleted(ids = ids))
        }
        return count
    }

    /**
     * 从 update 入参抽 code（这里 id 即 code）；要求实现 [IIdEntity] 且 id 是 String。
     *
     * @param any 更新入参
     * @return 系统 code
     * @throws IllegalStateException 入参类型不被支持
     * @author K
     * @since 1.0.0
     */
    private fun requireSystemCode(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新系统时不支持的入参类型: ${any::class.qualifiedName}")
}
