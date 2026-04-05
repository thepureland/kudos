package io.kudos.ms.sys.core.dict.service.iservice
import java.util.LinkedHashMap

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.core.dict.model.po.SysDictItem


/**
 * 字典项业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysDictItemService : IBaseCrudService<String, SysDictItem> {

    /**
     * 按主键从 Hash 缓存加载单条字典项
     */
    fun getDictItemFromCache(id: String): SysDictItemCacheEntry?

    /**
     * 按字典类型 + 原子服务编码从 Hash 缓存加载字典项列表
     */
    fun getDictItemsFromCache(dictType: String, atomicServiceCode: String): List<SysDictItemCacheEntry>

    /**
     * 批量按「原子服务 → 字典类型集合」从缓存加载字典项
     */
    fun batchGetDictItemsFromCache(
        dictTypesByAtomicServiceCode: Map<String, Collection<String>>
    ): Map<String, Map<String, List<SysDictItemCacheEntry>>>

    /**
     * 字典项编码 → 名称（来自缓存列表）
     */
    fun getDictItemMapFromCache(dictType: String, atomicServiceCode: String): LinkedHashMap<String, String>

    /**
     * 批量字典项编码 → 名称映射
     */
    fun batchGetDictItemMapFromCache(
        dictTypesByAtomicServiceCode: Map<String, Collection<String>>,
    ): Map<String, Map<String, LinkedHashMap<String, String>>>

    /**
     * 将字典项代码译为名称（查缓存列表）
     */
    fun transDictItemNameFromCache(dictType: String, itemCode: String, atomicServiceCode: String): String?

    /**
     * 获取字典项的所有祖先 id（DAO 父链）
     */
    fun fetchAllParentIds(itemId: String): List<String>

    /**
     * 删除字典项并级联删除其所有孩子
     */
    fun cascadeDeleteChildren(id: String): Boolean

    /**
     * 更新启用状态，并同步缓存
     */
    fun updateActive(dictItemId: String, active: Boolean): Boolean

    /**
     * 移动字典项（调整父节点和排序）
     */
    fun moveItem(id: String, newParentId: String?, newOrderNum: Int?): Boolean

    /**
     * 指定字典类型下第一层字典项（parentId 为空）
     */
    fun getDirectChildrenOfDictFromCache(
        atomicServiceCode: String,
        dictType: String,
        activeOnly: Boolean = true
    ): List<SysDictItemCacheEntry>

    /**
     * 指定字典项编码下的直接子项
     */
    fun getDirectChildrenOfItemFromCache(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String,
        activeOnly: Boolean = true
    ): List<SysDictItemCacheEntry>

    /**
     * 指定父字典项 id 下的直接子项
     */
    fun getDirectChildrenOfItemFromCache(parentId: String, activeOnly: Boolean = true): List<SysDictItemCacheEntry>

}
