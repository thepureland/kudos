package io.kudos.ms.sys.core.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ms.sys.common.api.ISysDictApi
import io.kudos.ms.sys.common.vo.dict.SysDictCacheItem
import io.kudos.ms.sys.common.vo.dict.SysDictPayload
import io.kudos.ms.sys.common.vo.dict.SysDictRecord
import io.kudos.ms.sys.core.model.po.SysDict


/**
 * 字典业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface ISysDictService : IBaseCrudService<String, SysDict>, ISysDictApi {
//endregion your codes 1

    //region your codes 2

    fun getDictFromCache(dictId: String): SysDictCacheItem?


//    /**
//     * 查询符合条件的字典项及字典
//     *
//     * @param searchPayload 查询参数
//     * @return Pair(List(SysDictListModel), 总记录数)
//     * @author K
//     * @since 1.0.0
//     */
//    fun pagingSearch(searchPayload: SysDictSearchPayload): Pair<List<RegDictRecord>, Int>

    /**
     * 返回指定id的字典信息
     *
     * @param id 字典id
     * @return SysDictRecord，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getRecord(id: String): SysDictRecord?

    /**
     * 保存或更新字典或字典项
     *
     * @param payload 数据载体
     * @return 主键
     * @author K
     * @since 1.0.0
     */
    fun saveOrUpdate(payload: SysDictPayload): String

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
     * @return 字典记录列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getDictsByAtomicServiceCode(atomicServiceCode: String): List<SysDictRecord>

    /**
     * 根据原子服务编码和字典类型获取字典
     *
     * @param atomicServiceCode 原子服务编码
     * @param dictType 字典类型
     * @return 字典记录，找不到返回null
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getDictByAtomicServiceAndType(atomicServiceCode: String, dictType: String): SysDictRecord?

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

    //endregion your codes 2

}