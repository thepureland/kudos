package io.kudos.ms.sys.common.api

import io.kudos.ms.sys.common.vo.dict.SysDictForm
import io.kudos.ms.sys.common.vo.dict.SysDictTreeNode
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheEntry
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemRow
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemQuery
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemTreeRow


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

    fun get(id: String, fetchAllParentIds: Boolean = false): SysDictItemRow?

    fun getItemsFromCache(type: String, atomicServiceCode: String): List<SysDictItemCacheEntry>

    fun transDictCode(dictType: String, itemCode: String, atomicServiceCode: String): String?

    fun saveOrUpdate(payload: SysDictForm): String

    fun fetchAllParentIds(itemId: String): List<String>

    fun cascadeDeleteChildren(id: String): Boolean

    fun loadDirectChildrenForTree(parent: String?, isModule: Boolean, activeOnly: Boolean = true): List<SysDictTreeNode>

    fun loadDirectChildrenForList(searchPayload: SysDictItemQuery): Pair<List<SysDictItemRow>, Int>

    fun updateActive(dictItemId: String, active: Boolean): Boolean

    fun getDictItemsByDictId(dictId: String): List<SysDictItemRow>

    fun getDictItemsByAtomicServiceAndType(dictType: String, atomicServiceCode: String): List<SysDictItemCacheEntry>

    fun getDictItemTree(dictId: String, parentId: String? = null): List<SysDictItemTreeRow>

    fun getChildItems(parentId: String): List<SysDictItemRow>

    fun moveItem(id: String, newParentId: String?, newOrderNum: Int?): Boolean

    //endregion your codes 2

}