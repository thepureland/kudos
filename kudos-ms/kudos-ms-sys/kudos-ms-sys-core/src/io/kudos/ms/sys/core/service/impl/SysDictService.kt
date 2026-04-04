package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.vo.dict.SysDictCacheEntry
import io.kudos.ms.sys.common.vo.dict.response.SysDictRow
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheEntry
import io.kudos.ms.sys.core.cache.SysDictHashCache
import io.kudos.ms.sys.core.dao.SysDictDao
import io.kudos.ms.sys.core.model.po.SysDict
import io.kudos.ms.sys.core.service.iservice.ISysDictItemService
import io.kudos.ms.sys.core.service.iservice.ISysDictService
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
) : BaseCrudService<String, SysDict, SysDictDao>(dao), ISysDictService {

    private val log = LogFactory.getLog(this::class)

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        return if (returnType == SysDictCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            sysDictHashCache.getDictById(id) as R?
        } else {
            super.get(id, returnType)
        }
    }

    override fun getDictFromCache(dictId: String): SysDictCacheEntry? = sysDictHashCache.getDictById(dictId)

    override fun getRecord(id: String): SysDictRow? = dao.get(id)?.let(::toSysDictRow)

    @Transactional
    override fun delete(id: String, isDict: Boolean): Boolean =
        if (isDict) deleteDictWithItems(id) else sysDictItemService.cascadeDeleteChildren(id)

    override fun getDictsFromCacheByAtomicServiceCode(
        atomicServiceCode: String,
        activeOnly: Boolean
    ): List<SysDictCacheEntry> = sysDictHashCache.getDictsByAtomicServiceCode(atomicServiceCode)
        .let { dicts -> if (activeOnly) dicts.filter { it.active } else dicts }

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
            sysDictHashCache.syncOnUpdate(id)
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "新增id为${id}的字典。") {
            sysDictHashCache.syncOnInsert(any, id)
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
            sysDictHashCache.syncOnUpdate(id)
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
            sysDictHashCache.syncOnDelete(id)
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除字典，期望删除${ids.size}条，实际删除${count}条。")
        sysDictHashCache.syncOnBatchDelete(ids)
        return count
    }

    override fun getActiveDictItemsFromCache(
        dictType: String,
        atomicServiceCode: String,
    ): List<SysDictItemCacheEntry> = getActiveDictItems(dictType, atomicServiceCode)

    override fun getActiveDictItemMapFromCache(
        dictType: String,
        atomicServiceCode: String,
    ): LinkedHashMap<String, String> = getActiveDictItemMap(dictType, atomicServiceCode)

    override fun batchGetActiveDictItemsFromCache(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, List<SysDictItemCacheEntry>> =
        dictTypeAndASCodePairs.associate { (dictType, atomicServiceCode) ->
            dictCacheKey(dictType, atomicServiceCode) to getActiveDictItems(dictType, atomicServiceCode)
        }

    override fun batchGetActiveDictItemMapFromCache(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, LinkedHashMap<String, String>> =
        dictTypeAndASCodePairs.associate { (dictType, atomicServiceCode) ->
            dictCacheKey(dictType, atomicServiceCode) to getActiveDictItemMap(dictType, atomicServiceCode)
        }

    private fun deleteDictWithItems(id: String): Boolean {
        dao.deleteDictItemsByDictId(id)
        return deleteDict(id)
    }

    private fun deleteDict(id: String): Boolean {
        val success = dao.deleteById(id)
        if (success) {
            sysDictHashCache.syncOnDelete(id)
        } else {
            log.error("删除id为${id}的字典失败！")
        }
        return success
    }

    private fun getActiveDictItems(dictType: String, atomicServiceCode: String): List<SysDictItemCacheEntry> =
        sysDictItemService.getDictItemsFromCache(dictType, atomicServiceCode)

    private fun getActiveDictItemMap(dictType: String, atomicServiceCode: String): LinkedHashMap<String, String> =
        getActiveDictItems(dictType, atomicServiceCode).associateTo(LinkedHashMap()) { it.itemCode to it.itemName }

    private fun dictCacheKey(dictType: String, atomicServiceCode: String): Pair<String, String> =
        Pair(atomicServiceCode, dictType)

    private fun toSysDictRow(dict: SysDict): SysDictRow = SysDictRow(
        id = dict.id,
        dictType = dict.dictType,
        dictName = dict.dictName,
        atomicServiceCode = dict.atomicServiceCode,
        remark = dict.remark,
        active = dict.active,
        builtIn = dict.builtIn,
    )

    private fun requireDictId(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新字典时不支持的入参类型: ${any::class.qualifiedName}")
}
