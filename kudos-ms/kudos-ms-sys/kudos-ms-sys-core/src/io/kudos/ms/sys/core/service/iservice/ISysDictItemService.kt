package io.kudos.ms.sys.core.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ms.sys.common.vo.dict.SysDictForm
import io.kudos.ms.sys.common.vo.dict.SysDictTreeNode
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheEntry
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemRow
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemQuery
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemTreeRow
import io.kudos.ms.sys.core.model.po.SysDictItem


/**
 * 字典项业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysDictItemService : IBaseCrudService<String, SysDictItem> {
//endregion your codes 1

    //region your codes 2

    /**
     * 返回指定id的字典项信息
     *
     * @param id 字典项id
     * @param fetchAllParentIds 是否要获取所有父项id，默认为false
     * @return SysDictItemRow，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun get(id: String, fetchAllParentIds: Boolean = false): SysDictItemRow?

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
     * 保存或更新
     *
     * @param form 表单对象
     * @return 主键
     * @author K
     * @since 1.0.0
     */
    fun saveOrUpdate(form: SysDictForm): String

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
     * 加载直接孩子结点(用于树)
     *
     * @param parent 父项标识，为null时加载模块列表
     * @param isModule 是否parent代表模块名
     * @param activeOnly 是否只加载启用状态的数据, 默认为是
     * @return List(SysDictTreeNode)
     * @author K
     * @since 1.0.0
     */
    fun loadDirectChildrenForTree(parent: String?, isModule: Boolean, activeOnly: Boolean = true): List<SysDictTreeNode>

    /**
     * 加载直接孩子结点(用于列表)
     *
     * @param searchPayload 查询参数
     * @return Pair(List(SysDictItemRow), 总记录数)
     * @author K
     * @since 1.0.0
     */
    fun loadDirectChildrenForList(searchPayload: SysDictItemQuery): Pair<List<SysDictItemRow>, Int>

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
     * 获取字典的所有字典项
     *
     * @param dictId 字典id
     * @return 字典项记录列表
     * @author K
     * @since 1.0.0
     */
    fun getDictItemsByDictId(dictId: String): List<SysDictItemRow>

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
     * 获取字典项树（递归结构）
     *
     * @param dictId 字典id
     * @param parentId 父字典项id，为null时返回顶级字典项
     * @return 字典项树节点列表（树形结构，包含children字段）
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getDictItemTree(dictId: String, parentId: String? = null): List<SysDictItemTreeRow>

    /**
     * 获取子字典项列表
     *
     * @param parentId 父字典项id
     * @return 子字典项记录列表
     * @author K
     * @since 1.0.0
     */
    fun getChildItems(parentId: String): List<SysDictItemRow>

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

    //endregion your codes 2

}