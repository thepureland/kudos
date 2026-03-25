package io.kudos.ms.sys.core.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheEntry
import io.kudos.ms.sys.core.model.po.SysDictItem


/**
 * 字典项业务接口
 *
 * @author K
 * @since 1.0.0
 */
interface ISysDictItemService : IBaseCrudService<String, SysDictItem> {


    /**
     * 根据字典类型和原子服务编码取得对应的字典项
     *
     * @param dictType 字典类型
     * @param atomicServiceCode 原子服务编码
     * @return 字典项缓存对象列表
     */
    fun getItems(dictType: String, atomicServiceCode: String): List<SysDictItemCacheEntry>

    /**
     * 根据字典类型集合和原子服务编码取得对应的字典项
     *
     * @param dictTypesByAtomicServiceCode Map<原子服务编码，Collection<字典类型编码>>
     * @return Map<原子服务编码，Map<字典类型，字典项缓存对象列表>>
     */
    fun batchGetDictItems(
        dictTypesByAtomicServiceCode: Map<String, Collection<String>>
    ): Map<String, Map<String, List<SysDictItemCacheEntry>>>

    /**
     * 根据字典类型和原子服务编码取得对应的字典项
     *
     * @param dictType 字典类型
     * @param atomicServiceCode 原子服务编码
     * @return LinkedHashMap<字典项编码，字典项译文或其国际化key>
     */
    fun getItemMap(dictType: String, atomicServiceCode: String): LinkedHashMap<String, String>

    /**
     * 批量获取字典项信息
     *
     * @param dictTypesByAtomicServiceCode Map<原子服务编码，Collection<字典类型编码>>
     * @return Map<原子服务编码，Map<字典类型，LinkedHashMap<字典项编码，字典项译文或其国际化key>>>
     */
    fun batchGetDictItemMap(
        dictTypesByAtomicServiceCode: Map<String, Collection<String>>,
    ): Map<String, Map<String, LinkedHashMap<String, String>>>

    /**
     * 翻译字典项代码
     *
     * @param dictType 字典类型
     * @param itemCode 字典项代码
     * @param atomicServiceCode 原子服务编码
     * @return 字典项名称，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun transDictCode(dictType: String, itemCode: String, atomicServiceCode: String): String?

    /**
     * 获取字典项的所有祖先id
     *
     * @param itemId 字典项id
     * @return List(祖先id)
     * @author K
     * @since 1.0.0
     */
    fun fetchAllParentIds(itemId: String): List<String>

    /**
     * 删除字典项并级联删除其所有孩子
     *
     * @param id 字典项id
     * @return 是否删除成功
     * @author K
     * @since 1.0.0
     */
    fun cascadeDeleteChildren(id: String): Boolean

    /**
     * 更新启用状态，并同步缓存
     *
     * @param dictItemId 字典项id
     * @param active 是否启用
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun updateActive(dictItemId: String, active: Boolean): Boolean

    /**
     * 根据原子服务编码和字典类型从缓存获取字典项列表
     *
     * @param atomicServiceCode 原子服务编码
     * @param dictType 字典类型
     * @return 字典项缓存项列表
     * @author K
     * @since 1.0.0
     */
    fun getDictItemsByAtomicServiceAndType(dictType: String, atomicServiceCode: String): List<SysDictItemCacheEntry>

    /**
     * 移动字典项（调整父节点和排序）
     *
     * @param id 字典项id
     * @param newParentId 新的父字典项id，为null表示移动到顶级
     * @param newOrderNum 新的排序号
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun moveItem(id: String, newParentId: String?, newOrderNum: Int?): Boolean

    /**
     * 返回指定字典类型的直接孩子(第一层字典项)
     *
     * @param atomicServiceCode 原子服务编码
     * @param dictType 字典类型
     * @param activeOnly 仅启用，为false将包含未启用的
     * @return List<SysDictItemCacheEntry>
     */
    fun getDirectChildrenOfDict(
        atomicServiceCode: String,
        dictType: String,
        activeOnly: Boolean = true
    ): List<SysDictItemCacheEntry>

    /**
     * 返回指定字典项的直接孩子
     *
     * @param atomicServiceCode 原子服务编码
     * @param dictType 字典类型
     * @param itemCode 字典项编码
     * @param activeOnly 仅启用，为false将包含未启用的
     * @return List<SysDictItemCacheEntry>
     */
    fun getDirectChildrenOfItem(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String,
        activeOnly: Boolean = true
    ): List<SysDictItemCacheEntry>

    /**
     * 返回指定父字典项id的直接孩子
     *
     * @param parentId 父字典项id
     * @param activeOnly 仅启用，为false将包含未启用的
     * @return List<SysDictItemCacheEntry>
     */
    fun getDirectChildrenOfItem(parentId: String, activeOnly: Boolean = true): List<SysDictItemCacheEntry>


}