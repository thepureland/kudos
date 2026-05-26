package io.kudos.ms.sys.core.dict.service.impl

import io.kudos.base.query.PagingSearchResult
import io.kudos.base.support.service.impl.BaseReadOnlyService
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.common.dict.vo.request.SysDictItemQuery
import io.kudos.ms.sys.common.dict.vo.response.SysDictItemRow
import io.kudos.ms.sys.core.dict.cache.SysDictItemHashCache
import io.kudos.ms.sys.core.dict.dao.VSysDictItemDao
import io.kudos.ms.sys.core.dict.model.po.VSysDictItem
import io.kudos.ms.sys.core.dict.service.iservice.IVSysDictItemService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read-only service implementation for the dictionary item view (v_sys_dict_item).
 *
 * Data source: view v_sys_dict_item; provides read-only queries with no write operations.
 *
 * @author K
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
open class VSysDictItemService(
    dao: VSysDictItemDao,
    private val sysDictItemHashCache: SysDictItemHashCache,
) : BaseReadOnlyService<String, VSysDictItem, VSysDictItemDao>(dao),
    IVSysDictItemService {

    override fun fetchByAtomicServiceCodeAndDictTypeAndItemCode(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String
    ): SysDictItemCacheEntry? {
        return sysDictItemHashCache.getDictItem(atomicServiceCode, dictType, itemCode)
    }

    override fun searchByAtomicServiceCodeAndDictType(
        atomicServiceCode: String,
        dictType: String
    ): List<SysDictItemCacheEntry> {
        return sysDictItemHashCache.getDictItems(atomicServiceCode, dictType)
    }

    override fun searchByParentId(parentId: String): List<SysDictItemCacheEntry> {
        return sysDictItemHashCache.getDictItems(parentId)
    }

    override fun getFromCache(id: String): SysDictItemCacheEntry? {
        return sysDictItemHashCache.getDictItemById(id)
    }

    override fun searchByIds(ids: Set<String>): Map<String, SysDictItemCacheEntry> {
        return sysDictItemHashCache.getDictItemsByIds(ids)
    }

    @Suppress("UNCHECKED_CAST")
    override fun pagingSearchWithParentCode(
        payload: SysDictItemQuery
    ): PagingSearchResult<SysDictItemRow> {
        val result = pagingSearch(payload)
        val rows = result.data as List<SysDictItemRow>
        val parentIds = rows.mapNotNull { it.parentId }.toSet()
        if (parentIds.isNotEmpty()) {
            val parentMap = searchByIds(parentIds)
            if (parentMap.isNotEmpty()) {
                rows.forEach { row ->
                    row.parentCode = row.parentId?.let { parentMap[it]?.itemCode }
                }
            }
        }
        return result as PagingSearchResult<SysDictItemRow>
    }

}
