package io.kudos.ms.sys.core.dict.service.iservice

import io.kudos.base.support.service.iservice.IBaseReadOnlyService
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.core.dict.model.po.VSysDictItem

/**
 * Read-only service interface for the dictionary item view (v_sys_dict_item).
 *
 * Data source: view v_sys_dict_item (sys_dict_item left join sys_dict); provides query capability only.
 *
 * @author K
 * @since 1.0.0
 */
interface IVSysDictItemService : IBaseReadOnlyService<String, VSysDictItem> {

    /**
     * Query a dictionary item by atomic service code, dictionary type, item code and active status (at most one row).
     *
     * @param atomicServiceCode atomic service code
     * @param dictType dictionary type
     * @param itemCode dictionary item code
     * @return SysDictItemCacheEntry, or null if not found
     */
    fun fetchByAtomicServiceCodeAndDictTypeAndItemCode(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String
    ): SysDictItemCacheEntry?

    /**
     * Query the dictionary item list by atomic service code, dictionary type and active status (sorted by orderNum).
     *
     * @param atomicServiceCode atomic service code
     * @param dictType dictionary type
     * @return list of matching SysDictItemCacheEntry
     */
    fun searchByAtomicServiceCodeAndDictType(
        atomicServiceCode: String,
        dictType: String
    ): List<SysDictItemCacheEntry>

    /**
     * Query the child dictionary item list by parent dictionary item id and active status (sorted by orderNum).
     *
     * @param parentId parent dictionary item id, non-null
     * @return list of matching SysDictItemCacheEntry
     */
    fun searchByParentId(parentId: String): List<SysDictItemCacheEntry>

    /**
     * Return the cached dictionary item for the given id.
     *
     * @param id primary key
     * @return SysDictItemCacheEntry, or null if not found
     */
    fun getFromCache(id: String): SysDictItemCacheEntry?

    /**
     * Return the cached dictionary items corresponding to the given primary key set.
     *
     * @param ids primary key set
     * @return Map<id, SysDictItemCacheEntry>
     */
    fun searchByIds(ids: Set<String>): Map<String, SysDictItemCacheEntry>

    /**
     * Paginated dictionary item query; for matched rows with a non-null parentId, backfill the parentCode before
     * returning.
     *
     * Sinks the previously manual "query -> collect parentId -> second batch query -> backfill parentCode"
     * orchestration done in the controller into the service.
     *
     * @param payload pagination query parameters
     * @return matched paged result, where each row's `parentCode` has been backfilled when the parent dictionary item
     *         exists
     */
    fun pagingSearchWithParentCode(
        payload: io.kudos.ms.sys.common.dict.vo.request.SysDictItemQuery
    ): io.kudos.base.query.PagingSearchResult<io.kudos.ms.sys.common.dict.vo.response.SysDictItemRow>


}
