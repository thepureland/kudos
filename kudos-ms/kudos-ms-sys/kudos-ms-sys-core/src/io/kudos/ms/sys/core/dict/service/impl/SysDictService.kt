package io.kudos.ms.sys.core.dict.service.impl

import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.platform.service.impl.requireStringId
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
 * Dictionary business.
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
            successMessage = "Updated active status of dictionary with id ${id} to ${active}.",
            failureMessage = "Failed to update active status of dictionary with id ${id} to ${active}!",
        ) {
            eventPublisher.publishEvent(SysDictUpdated(id = id))
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "Inserted dictionary with id ${id}.") {
            eventPublisher.publishEvent(SysDictInserted(id = id))
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireStringId(any, "dictionary")
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "Updated dictionary with id ${id}.",
            failureMessage = "Failed to update dictionary with id ${id}!",
        ) {
            eventPublisher.publishEvent(SysDictUpdated(id = id))
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        if (dao.get(id) == null) {
            log.warn("When deleting dictionary with id ${id}, found that it no longer exists!")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "Deleted dictionary with id ${id}.",
            failureMessage = "Failed to delete dictionary with id ${id}!",
        ) {
            eventPublisher.publishEvent(SysDictDeleted(id = id))
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("Batch-deleted dictionaries: expected to delete ${ids.size}, actually deleted ${count}.")
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
     * Cascade delete: clear dictionary items first, then delete the dictionary itself.
     * Order cannot be swapped—deleting the dictionary first would trigger a foreign key constraint error
     * (dictionary item FK references dictionary id).
     *
     * @param id dictionary id
     * @return whether deletion of the dictionary itself succeeded (deletion of dictionary items is not included in the
     *         return value, but a failure would reverse-block via the constraint)
     * @author K
     * @since 1.0.0
     */
    private fun deleteDictWithItems(id: String): Boolean {
        dao.deleteDictItemsByDictId(id)
        return deleteDict(id)
    }

    /**
     * Single dictionary deletion: DAO removes the row -> on success, publishes [SysDictDeleted] for downstream cache
     * cleanup; on failure, logs at ERROR only.
     *
     * @param id dictionary id
     * @return whether a row was actually deleted
     * @author K
     * @since 1.0.0
     */
    private fun deleteDict(id: String): Boolean {
        val success = dao.deleteById(id)
        if (success) {
            eventPublisher.publishEvent(SysDictDeleted(id = id))
        } else {
            log.error("Failed to delete dictionary with id ${id}!")
        }
        return success
    }

    /**
     * Get the active dictionary items cache (active=true is already filtered by `SysDictItemService` on the cache side).
     *
     * @param dictType dictionary type
     * @param atomicServiceCode atomic service code (multi-tenant / multi-service isolation key)
     * @return list of dictionary item cache entries
     * @author K
     * @since 1.0.0
     */
    private fun getActiveDictItems(dictType: String, atomicServiceCode: String): List<SysDictItemCacheEntry> =
        sysDictItemService.getDictItemsFromCache(dictType, atomicServiceCode)

    /**
     * itemCode -> itemName mapping for active dictionary items; uses [LinkedHashMap] to preserve insertion order so
     * the business sort is maintained.
     *
     * @param dictType dictionary type
     * @param atomicServiceCode atomic service code
     * @return ordered itemCode -> itemName mapping
     * @author K
     * @since 1.0.0
     */
    private fun getActiveDictItemMap(dictType: String, atomicServiceCode: String): LinkedHashMap<String, String> =
        getActiveDictItems(dictType, atomicServiceCode).associateTo(LinkedHashMap()) { it.itemCode to it.itemName }

    /**
     * Build the key pair for batch dictionary cache hits: `(atomicServiceCode, dictType)`.
     * **Note the order**—batch inputs are `(dictType, atomicServiceCode)`; this intentionally flips them to stay
     * consistent with the key order of `batchGetActiveDictItemMapFromCache`'s return value, preventing callers from
     * swapping positions.
     *
     * @param dictType dictionary type
     * @param atomicServiceCode atomic service code
     * @return Pair in `(atomicServiceCode, dictType)` order
     * @author K
     * @since 1.0.0
     */
    private fun dictCacheKey(dictType: String, atomicServiceCode: String): Pair<String, String> =
        Pair(atomicServiceCode, dictType)

    /**
     * Copies the PO [SysDict] into the flat VO [SysDictRow] for the list endpoint (avoids exposing ORM Entity fields).
     *
     * @param dict dictionary PO
     * @return dictionary VO
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

}
