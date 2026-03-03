package io.kudos.ms.sys.common.api

import io.kudos.ms.sys.common.vo.dict.SysDictPayload
import io.kudos.ms.sys.common.vo.dict.SysDictTreeNode
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheItem
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemRecord
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemSearchPayload
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemTreeRecord


/**
 * 字典项 对外API
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysDictItemApi {
//endregion your codes 1

    //region your codes 2

    fun get(id: String, fetchAllParentIds: Boolean = false): SysDictItemRecord?

    fun getItemsFromCache(atomicServiceCode: String? = null, type: String): List<SysDictItemCacheItem>

    fun transDictCode(module: String, type: String, code: String): String?

    fun saveOrUpdate(payload: SysDictPayload): String

    fun fetchAllParentIds(itemId: String): List<String>

    fun cascadeDeleteChildren(id: String): Boolean

    fun loadDirectChildrenForTree(parent: String?, isModule: Boolean, activeOnly: Boolean = true): List<SysDictTreeNode>

    fun loadDirectChildrenForList(searchPayload: SysDictItemSearchPayload): Pair<List<SysDictItemRecord>, Int>

    fun updateActive(dictItemId: String, active: Boolean): Boolean

    fun getDictItemsByDictId(dictId: String): List<SysDictItemRecord>

    fun getDictItemsByAtomicServiceAndType(atomicServiceCode: String, dictType: String): List<SysDictItemCacheItem>

    fun getDictItemTree(dictId: String, parentId: String? = null): List<SysDictItemTreeRecord>

    fun getChildItems(parentId: String): List<SysDictItemRecord>

    fun moveItem(id: String, newParentId: String?, newOrderNum: Int?): Boolean

    //endregion your codes 2

}