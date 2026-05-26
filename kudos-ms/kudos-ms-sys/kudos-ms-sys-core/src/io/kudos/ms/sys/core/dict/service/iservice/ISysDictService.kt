package io.kudos.ms.sys.core.dict.service.iservice

import java.util.LinkedHashMap

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.dict.vo.SysDictCacheEntry
import io.kudos.ms.sys.common.dict.vo.response.SysDictRow
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.core.dict.model.po.SysDict


/**
 * Dictionary business interface.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysDictService : IBaseCrudService<String, SysDict> {

    /**
     * Load a dictionary configuration from the Hash cache by dictionary primary key.
     */
    fun getDictFromCache(dictId: String): SysDictCacheEntry?

    /**
     * Fetch the dictionary list from the cache by atomic service code; when [activeOnly] is true, only active entries
     * are kept (in-memory filtering).
     */
    fun getDictsFromCacheByAtomicServiceCode(atomicServiceCode: String, activeOnly: Boolean = true): List<SysDictCacheEntry>

    /**
     * Get the mapping from dictionary id to dictionary type by atomic service code.
     *
     * @param atomicServiceCode atomic service code
     * @param activeOnly when true, return only active entries
     * @return Map<dictionary id, dictionary type>
     */
    fun getDictTypesByAtomicServiceCode(atomicServiceCode: String, activeOnly: Boolean = true): Map<String, String>

    /**
     * Query the database directly by atomic service code and dictionary type to get the list row.
     */
    fun getDictByAtomicServiceAndType(atomicServiceCode: String, dictType: String): SysDictRow?

    /**
     * Query the database directly by id to get the list row.
     */
    fun getRecord(id: String): SysDictRow?

    /**
     * Delete a dictionary or a dictionary item.
     *
     * @param id primary key
     * @param isDict true: dictionary id; false: dictionary item id
     */
    fun delete(id: String, isDict: Boolean): Boolean

    /**
     * Update the active status and sync the cache.
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * Get active dictionary items by dictionary type and atomic service code (via the dictionary item cache).
     */
    fun getActiveDictItemsFromCache(dictType: String, atomicServiceCode: String): List<SysDictItemCacheEntry>

    /**
     * Get the active dictionary items code->name mapping by dictionary type and atomic service code.
     */
    fun getActiveDictItemMapFromCache(
        dictType: String,
        atomicServiceCode: String
    ): LinkedHashMap<String, String>

    /**
     * Batch-get active dictionary items.
     */
    fun batchGetActiveDictItemsFromCache(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, List<SysDictItemCacheEntry>>

    /**
     * Batch-get the active dictionary items code->name mapping.
     */
    fun batchGetActiveDictItemMapFromCache(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, LinkedHashMap<String, String>>

}
