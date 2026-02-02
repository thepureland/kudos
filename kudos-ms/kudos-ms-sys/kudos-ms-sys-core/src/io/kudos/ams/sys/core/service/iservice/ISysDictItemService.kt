package io.kudos.ms.sys.core.service.iservice

import io.kudos.ms.sys.common.vo.dict.SysDictPayload
import io.kudos.ms.sys.common.vo.dict.SysDictTreeNode
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheItem
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemRecord
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemSearchPayload
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemTreeRecord
import io.kudos.ms.sys.core.model.po.SysDictItem
import io.kudos.base.support.iservice.IBaseCrudService


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
     * @return SysDictItemRecord，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun get(id: String, fetchAllParentIds: Boolean = false): SysDictItemRecord?

    /**
     * 根据模块和字典类型，取得对应字典项(仅包括处于启用状态的)，并将结果缓存，查不到不缓存
     *
     * @param module 如果没有请传入空串，此时请保证type的惟一性，否则结果将不确定是哪条记录
     * @param type 字典类型
     * @return 字典项列表。如果module为空串，且存在多个同名type，将任意返回一个type对应的字典项。查无结果返回空列表。
     * @author K
     * @since 1.0.0
     */
    fun getItemsFromCache(module: String, type: String): List<SysDictItemCacheItem>

    /**
     * 翻译字典项代码
     *
     * @param module 如果没有请传入空串，此时请保证type的惟一性，否则结果将不确定是哪条记录
     * @param type 字典类型
     * @param code 字典项代码
     * @return 字典项名称，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun transDictCode(module: String, type: String, code: String): String?

    /**
     * 保存或更新
     *
     * @param payload 载体对象
     * @return 主键
     * @author K
     * @since 1.0.0
     */
    fun saveOrUpdate(payload: SysDictPayload): String

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
     * @return Pair(List(SysDictItemRecord), 总记录数)
     * @author K
     * @since 1.0.0
     */
    fun loadDirectChildrenForList(searchPayload: SysDictItemSearchPayload): Pair<List<SysDictItemRecord>, Int>

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
    fun getDictItemsByDictId(dictId: String): List<SysDictItemRecord>

    /**
     * 根据原子服务编码和字典类型从缓存获取字典项列表
     *
     * @param atomicServiceCode 原子服务编码
     * @param dictType 字典类型
     * @return 字典项缓存项列表
     * @author K
     * @since 1.0.0
     */
    fun getDictItemsByAtomicServiceAndType(atomicServiceCode: String, dictType: String): List<SysDictItemCacheItem>

    /**
     * 获取字典项树（递归结构）
     *
     * @param dictId 字典id
     * @param parentId 父字典项id，为null时返回顶级字典项
     * @return 字典项树节点列表（树形结构，包含children字段）
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getDictItemTree(dictId: String, parentId: String? = null): List<SysDictItemTreeRecord>

    /**
     * 获取子字典项列表
     *
     * @param parentId 父字典项id
     * @return 子字典项记录列表
     * @author K
     * @since 1.0.0
     */
    fun getChildItems(parentId: String): List<SysDictItemRecord>

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