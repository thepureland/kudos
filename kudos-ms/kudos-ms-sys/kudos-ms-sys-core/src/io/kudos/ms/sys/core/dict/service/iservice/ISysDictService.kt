package io.kudos.ms.sys.core.dict.service.iservice
import java.util.LinkedHashMap

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.dict.vo.SysDictCacheEntry
import io.kudos.ms.sys.common.dict.vo.response.SysDictRow
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.core.dict.model.po.SysDict


/**
 * 字典业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysDictService : IBaseCrudService<String, SysDict> {

    /**
     * 按字典主键从 Hash 缓存加载字典配置
     */
    fun getDictFromCache(dictId: String): SysDictCacheEntry?

    /**
     * 按原子服务编码从缓存取字典列表；[activeOnly] 为 true 时仅保留启用项（内存过滤）
     */
    fun getDictsFromCacheByAtomicServiceCode(atomicServiceCode: String, activeOnly: Boolean = true): List<SysDictCacheEntry>

    /**
     * 根据原子服务编码和字典类型直查库得到列表行
     */
    fun getDictByAtomicServiceAndType(atomicServiceCode: String, dictType: String): SysDictRow?

    /**
     * 根据 id 直查库得到列表行
     */
    fun getRecord(id: String): SysDictRow?

    /**
     * 删除字典或字典项
     *
     * @param id 主键
     * @param isDict true: 字典 id，false：字典项 id
     */
    fun delete(id: String, isDict: Boolean): Boolean

    /**
     * 更新启用状态，并同步缓存
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * 根据字典类型和原子服务编码取启用字典项（经字典项缓存）
     */
    fun getActiveDictItemsFromCache(dictType: String, atomicServiceCode: String): List<SysDictItemCacheEntry>

    /**
     * 根据字典类型和原子服务编码取启用字典项 code→name 映射
     */
    fun getActiveDictItemMapFromCache(
        dictType: String,
        atomicServiceCode: String
    ): LinkedHashMap<String, String>

    /**
     * 批量取启用字典项
     */
    fun batchGetActiveDictItemsFromCache(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, List<SysDictItemCacheEntry>>

    /**
     * 批量取启用字典项 code→name 映射
     */
    fun batchGetActiveDictItemMapFromCache(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, LinkedHashMap<String, String>>

}
