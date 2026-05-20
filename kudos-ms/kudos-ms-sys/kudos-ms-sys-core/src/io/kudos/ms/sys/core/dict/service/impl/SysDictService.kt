package io.kudos.ms.sys.core.dict.service.impl

import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.dict.vo.SysDictCacheEntry
import io.kudos.ms.sys.common.dict.vo.response.SysDictRow
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.core.dict.cache.SysDictHashCache
import io.kudos.ms.sys.core.dict.dao.SysDictDao
import io.kudos.ms.sys.core.dict.event.SysDictBatchDeleted
import io.kudos.ms.sys.core.dict.event.SysDictDeleted
import io.kudos.ms.sys.core.dict.event.SysDictInserted
import io.kudos.ms.sys.core.dict.event.SysDictUpdated
import io.kudos.ms.sys.core.dict.model.po.SysDict
import io.kudos.ms.sys.core.dict.service.iservice.ISysDictItemService
import io.kudos.ms.sys.core.dict.service.iservice.ISysDictService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.LinkedHashMap
import kotlin.reflect.KClass


/**
 * 字典业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class SysDictService(
    dao: SysDictDao,
    private val sysDictItemService: ISysDictItemService,
    private val sysDictHashCache: SysDictHashCache,
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, SysDict, SysDictDao>(dao), ISysDictService {

    private val log = LogFactory.getLog(this::class)

    @Suppress("UNCHECKED_CAST")
    @Transactional(readOnly = true)
    override fun <R : Any> get(id: String, returnType: KClass<R>): R? =
        if (returnType == SysDictCacheEntry::class) sysDictHashCache.getDictById(id) as R?
        else super.get(id, returnType)

    @Transactional(readOnly = true)
    override fun getDictFromCache(dictId: String): SysDictCacheEntry? = sysDictHashCache.getDictById(dictId)

    @Transactional(readOnly = true)
    override fun getRecord(id: String): SysDictRow? = dao.get(id)?.let(::toSysDictRow)

    @Transactional
    override fun delete(id: String, isDict: Boolean): Boolean =
        if (isDict) deleteDictWithItems(id) else sysDictItemService.cascadeDeleteChildren(id)

    @Transactional(readOnly = true)
    override fun getDictsFromCacheByAtomicServiceCode(
        atomicServiceCode: String,
        activeOnly: Boolean
    ): List<SysDictCacheEntry> = sysDictHashCache.getDictsByAtomicServiceCode(atomicServiceCode)
        .let { dicts -> if (activeOnly) dicts.filter { it.active } else dicts }

    @Transactional(readOnly = true)
    override fun getDictTypesByAtomicServiceCode(
        atomicServiceCode: String,
        activeOnly: Boolean
    ): Map<String, String> =
        getDictsFromCacheByAtomicServiceCode(atomicServiceCode, activeOnly).associate { it.id to it.dictType }

    @Transactional(readOnly = true)
    override fun getDictByAtomicServiceAndType(atomicServiceCode: String, dictType: String): SysDictRow? =
        dao.search(
            Criteria.and(
            SysDict::atomicServiceCode eq atomicServiceCode,
            SysDict::dictType eq dictType
        )
        ).firstOrNull()?.let(::toSysDictRow)

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val dict = SysDict {
            this.id = id
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(dict),
            log = log,
            successMessage = "更新id为${id}的字典的启用状态为${active}。",
            failureMessage = "更新id为${id}的字典的启用状态为${active}失败！",
        ) {
            eventPublisher.publishEvent(SysDictUpdated(id = id))
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "新增id为${id}的字典。") {
            eventPublisher.publishEvent(SysDictInserted(id = id))
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireDictId(any)
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "更新id为${id}的字典。",
            failureMessage = "更新id为${id}的字典失败！",
        ) {
            eventPublisher.publishEvent(SysDictUpdated(id = id))
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        if (dao.get(id) == null) {
            log.warn("删除id为${id}的字典时，发现其已不存在！")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "删除id为${id}的字典。",
            failureMessage = "删除id为${id}的字典失败！",
        ) {
            eventPublisher.publishEvent(SysDictDeleted(id = id))
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除字典，期望删除${ids.size}条，实际删除${count}条。")
        if (count > 0) {
            eventPublisher.publishEvent(SysDictBatchDeleted(ids = ids))
        }
        return count
    }

    @Transactional(readOnly = true)
    override fun getActiveDictItemsFromCache(
        dictType: String,
        atomicServiceCode: String,
    ): List<SysDictItemCacheEntry> = getActiveDictItems(dictType, atomicServiceCode)

    @Transactional(readOnly = true)
    override fun getActiveDictItemMapFromCache(
        dictType: String,
        atomicServiceCode: String,
    ): LinkedHashMap<String, String> = getActiveDictItemMap(dictType, atomicServiceCode)

    @Transactional(readOnly = true)
    override fun batchGetActiveDictItemsFromCache(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, List<SysDictItemCacheEntry>> =
        dictTypeAndASCodePairs.associate { (dictType, atomicServiceCode) ->
            dictCacheKey(dictType, atomicServiceCode) to getActiveDictItems(dictType, atomicServiceCode)
        }

    @Transactional(readOnly = true)
    override fun batchGetActiveDictItemMapFromCache(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, LinkedHashMap<String, String>> =
        dictTypeAndASCodePairs.associate { (dictType, atomicServiceCode) ->
            dictCacheKey(dictType, atomicServiceCode) to getActiveDictItemMap(dictType, atomicServiceCode)
        }

    /**
     * 级联删除：先清字典项再删字典本身。
     * 顺序不可换——先删字典会触发外键约束错误（字典项 FK 引用字典 id）。
     *
     * @param id 字典 id
     * @return 字典本身删除是否成功（字典项删除不计入返回值，但失败会因为约束反向阻断）
     * @author K
     * @since 1.0.0
     */
    private fun deleteDictWithItems(id: String): Boolean {
        dao.deleteDictItemsByDictId(id)
        return deleteDict(id)
    }

    /**
     * 单字典删除：DAO 删行 → 成功则发 [SysDictDeleted] 事件供下游清缓存；失败仅 ERROR 日志。
     *
     * @param id 字典 id
     * @return 是否真的删到行
     * @author K
     * @since 1.0.0
     */
    private fun deleteDict(id: String): Boolean {
        val success = dao.deleteById(id)
        if (success) {
            eventPublisher.publishEvent(SysDictDeleted(id = id))
        } else {
            log.error("删除id为${id}的字典失败！")
        }
        return success
    }

    /**
     * 取生效字典项缓存（active=true 已由 `SysDictItemService` 缓存侧筛过）。
     *
     * @param dictType 字典类型
     * @param atomicServiceCode 原子服务编码（多租户/多服务隔离 key）
     * @return 字典项缓存条目列表
     * @author K
     * @since 1.0.0
     */
    private fun getActiveDictItems(dictType: String, atomicServiceCode: String): List<SysDictItemCacheEntry> =
        sysDictItemService.getDictItemsFromCache(dictType, atomicServiceCode)

    /**
     * 生效字典项的 itemCode → itemName 映射；用 [LinkedHashMap] 保留插入顺序以维持业务排序。
     *
     * @param dictType 字典类型
     * @param atomicServiceCode 原子服务编码
     * @return itemCode → itemName 的有序映射
     * @author K
     * @since 1.0.0
     */
    private fun getActiveDictItemMap(dictType: String, atomicServiceCode: String): LinkedHashMap<String, String> =
        getActiveDictItems(dictType, atomicServiceCode).associateTo(LinkedHashMap()) { it.itemCode to it.itemName }

    /**
     * 构造字典批量缓存命中的 key 对：`(atomicServiceCode, dictType)`。
     * **注意顺序**——批量入参是 `(dictType, atomicServiceCode)`，这里特意翻转，
     * 与下游 `batchGetActiveDictItemMapFromCache` 返回值的 key 顺序保持一致，避免调用方对错位置。
     *
     * @param dictType 字典类型
     * @param atomicServiceCode 原子服务编码
     * @return `(atomicServiceCode, dictType)` 顺序的 Pair
     * @author K
     * @since 1.0.0
     */
    private fun dictCacheKey(dictType: String, atomicServiceCode: String): Pair<String, String> =
        Pair(atomicServiceCode, dictType)

    /**
     * 把 PO [SysDict] 拷成扁平的 VO [SysDictRow]，用于 list 接口（避免暴露 ORM Entity 字段）。
     *
     * @param dict 字典 PO
     * @return 字典 VO
     * @author K
     * @since 1.0.0
     */
    private fun toSysDictRow(dict: SysDict): SysDictRow = SysDictRow(
        id = dict.id,
        dictType = dict.dictType,
        dictName = dict.dictName,
        atomicServiceCode = dict.atomicServiceCode,
        remark = dict.remark,
        active = dict.active,
        builtIn = dict.builtIn,
    )

    /**
     * 从 update 入参抽 id；要求入参实现 [IIdEntity] 且 id 是 String。
     *
     * @param any 更新入参
     * @return 字典 id
     * @throws IllegalStateException 入参类型不被支持
     * @author K
     * @since 1.0.0
     */
    private fun requireDictId(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新字典时不支持的入参类型: ${any::class.qualifiedName}")
}
