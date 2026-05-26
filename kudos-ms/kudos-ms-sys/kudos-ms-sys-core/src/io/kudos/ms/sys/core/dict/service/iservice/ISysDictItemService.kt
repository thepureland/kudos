package io.kudos.ms.sys.core.dict.service.iservice

import java.util.LinkedHashMap

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.common.dict.vo.response.SysDictItemNode
import io.kudos.ms.sys.core.dict.model.po.SysDictItem


/**
 * Dictionary item business interface.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysDictItemService : IBaseCrudService<String, SysDictItem> {

    /**
     * Load a single dictionary item from the Hash cache by primary key.
     */
    fun getDictItemFromCache(id: String): SysDictItemCacheEntry?

    /**
     * Load the dictionary item list from the Hash cache by dictionary type + atomic service code.
     */
    fun getDictItemsFromCache(dictType: String, atomicServiceCode: String): List<SysDictItemCacheEntry>

    /**
     * Batch-load dictionary items from the cache by "atomic service -> dictionary type set".
     */
    fun batchGetDictItemsFromCache(
        dictTypesByAtomicServiceCode: Map<String, Collection<String>>
    ): Map<String, Map<String, List<SysDictItemCacheEntry>>>

    /**
     * Dictionary item code -> name (from the cached list).
     */
    fun getDictItemMapFromCache(dictType: String, atomicServiceCode: String): LinkedHashMap<String, String>

    /**
     * Batch dictionary item code -> name mapping.
     */
    fun batchGetDictItemMapFromCache(
        dictTypesByAtomicServiceCode: Map<String, Collection<String>>,
    ): Map<String, Map<String, LinkedHashMap<String, String>>>

    /**
     * Translate a dictionary item code to its name (looking up the cached list).
     */
    fun transDictItemNameFromCache(dictType: String, itemCode: String, atomicServiceCode: String): String?

    /**
     * Get all ancestor ids of a dictionary item (DAO parent chain).
     */
    fun fetchAllParentIds(itemId: String): List<String>

    /**
     * Delete the dictionary item and cascade-delete all of its children.
     */
    fun cascadeDeleteChildren(id: String): Boolean

    /**
     * Update the active status and sync the cache.
     */
    fun updateActive(dictItemId: String, active: Boolean): Boolean

    /**
     * Move the dictionary item (adjust parent node and ordering).
     */
    fun moveItem(id: String, newParentId: String?, newOrderNum: Int?): Boolean

    /**
     * First-level dictionary items under the given dictionary type (parentId is null).
     */
    fun getDirectChildrenOfDictFromCache(
        atomicServiceCode: String,
        dictType: String,
        activeOnly: Boolean = true
    ): List<SysDictItemCacheEntry>

    /**
     * Direct children under the given dictionary item code.
     */
    fun getDirectChildrenOfItemFromCache(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String,
        activeOnly: Boolean = true
    ): List<SysDictItemCacheEntry>

    /**
     * Direct children under the given parent dictionary item id.
     */
    fun getDirectChildrenOfItemFromCache(parentId: String, activeOnly: Boolean = true): List<SysDictItemCacheEntry>

    /**
     * id/code/name projection of the first-level dictionary items under the given dictionary type.
     */
    fun getDirectChildrenOfDictAsNodes(
        atomicServiceCode: String,
        dictType: String,
        activeOnly: Boolean = true,
    ): List<SysDictItemNode>

    /**
     * id/code/name projection of the direct children under the given dictionary item code.
     */
    fun getDirectChildrenOfItemAsNodes(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String,
        activeOnly: Boolean = true,
    ): List<SysDictItemNode>

}
