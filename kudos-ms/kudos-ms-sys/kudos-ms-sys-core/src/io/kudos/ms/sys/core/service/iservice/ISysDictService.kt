package io.kudos.ms.sys.core.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ms.sys.common.api.ISysDictApi
import io.kudos.ms.sys.common.vo.dict.SysDictCacheEntry
import io.kudos.ms.sys.common.vo.dict.SysDictRow
import io.kudos.ms.sys.core.model.po.SysDict


/**
 * 字典业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysDictService : IBaseCrudService<String, SysDict>, ISysDictApi {


    fun getFromCache(dictId: String): SysDictCacheEntry?

    /**
     * 返回指定id的字典信息
     *
     * @param id 字典id
     * @return SysDictRow，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getRecord(id: String): SysDictRow?

    /**
     * 删除字典或字典项
     *
     * @param id 主键
     * @param isDict true: 字典id，false：字典项id
     * @return 是否删除成功
     * @author K
     * @since 1.0.0
     */
    fun delete(id: String, isDict: Boolean): Boolean

    /**
     * 获取模块的所有字典
     *
     * @param atomicServiceCode 原子服务编码
     * @param activeOnly 仅启用，为null或false将包含未启用的
     * @return List<SysDictCacheEntry>
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getDictsByAtomicServiceCode(atomicServiceCode: String, activeOnly: Boolean = true): List<SysDictCacheEntry>

    /**
     * 根据原子服务编码和字典类型获取字典
     *
     * @param atomicServiceCode 原子服务编码
     * @param dictType 字典类型
     * @return 字典记录，找不到返回null
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getDictByAtomicServiceAndType(atomicServiceCode: String, dictType: String): SysDictRow?

    /**
     * 更新启用状态，并同步缓存
     *
     * @param id 字典id
     * @param active 是否启用
     * @return 是否更新成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun updateActive(id: String, active: Boolean): Boolean


}