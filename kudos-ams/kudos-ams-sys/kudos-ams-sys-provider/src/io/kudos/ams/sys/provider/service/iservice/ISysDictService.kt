package io.kudos.ams.sys.provider.service.iservice

import io.kudos.ams.sys.common.vo.dict.SysDictCacheItem
import io.kudos.ams.sys.common.vo.dict.SysDictPayload
import io.kudos.ams.sys.common.vo.dict.SysDictRecord
import io.kudos.ams.sys.provider.model.po.SysDict
import io.kudos.base.support.iservice.IBaseCrudService


/**
 * 字典业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysDictService : IBaseCrudService<String, SysDict> {
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

    //endregion your codes 2

}