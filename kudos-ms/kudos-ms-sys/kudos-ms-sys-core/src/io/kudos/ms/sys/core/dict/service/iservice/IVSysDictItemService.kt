package io.kudos.ms.sys.core.dict.service.iservice

import io.kudos.base.support.service.iservice.IBaseReadOnlyService
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.core.dict.model.po.VSysDictItem

/**
 * 字典项视图（v_sys_dict_item）只读服务接口。
 *
 * 数据来源：视图 v_sys_dict_item（sys_dict_item left join sys_dict），仅提供查询能力。
 *
 * @author K
 * @since 1.0.0
 */
interface IVSysDictItemService : IBaseReadOnlyService<String, VSysDictItem> {

    /**
     * 按原子服务编码、字典类型、字典项代码及启用状态查询字典项（至多一条）。
     *
     * @param atomicServiceCode 原子服务编码
     * @param dictType 字典类型
     * @param itemCode 字典项代码
     * @return SysDictItemCacheEntry，不存在返回 null
     */
    fun fetchByAtomicServiceCodeAndDictTypeAndItemCode(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String
    ): SysDictItemCacheEntry?

    /**
     * 按原子服务编码、字典类型及启用状态查询字典项列表（按 orderNum 排序）。
     *
     * @param atomicServiceCode 原子服务编码
     * @param dictType 字典类型
     * @return 匹配的 SysDictItemCacheEntry 列表
     */
    fun searchByAtomicServiceCodeAndDictType(
        atomicServiceCode: String,
        dictType: String
    ): List<SysDictItemCacheEntry>

    /**
     * 按父字典项 id 及启用状态查询子字典项列表（按 orderNum 排序）。
     *
     * @param parentId 父字典项 id，非空
     * @return 匹配的 SysDictItemCacheEntry 列表
     */
    fun searchByParentId(parentId: String): List<SysDictItemCacheEntry>

    /**
     * 返回指定id的字典项缓存
     *
     * @param id 主键
     * @return SysDictItemCacheEntry，不存在返回null
     */
    fun getFromCache(id: String): SysDictItemCacheEntry?

    /**
     * 返回主键集合对应的字典项缓存
     *
     * @param ids 主键集合
     * @return Map<id, SysDictItemCacheEntry>
     */
    fun searchByIds(ids: Set<String>): Map<String, SysDictItemCacheEntry>


}
