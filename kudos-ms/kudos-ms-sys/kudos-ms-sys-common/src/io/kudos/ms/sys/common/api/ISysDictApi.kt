package io.kudos.ms.sys.common.api

import io.kudos.ms.sys.common.vo.dict.DictTypeAndASCodePayload
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheItem


/**
 * 字典 对外API
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysDictApi {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据字典类型和原子服务编码，取得对应字典项(仅包括处于启用状态的)
     *
     * @param payload 字典类型和原子服务编码的载体列表
     * @return 字典项列表（自然排序）。查无结果返回空列表。
     * @throws IllegalArgumentException 参数校验不通过时
     * @author K
     * @since 1.0.0
     */
    fun getDictItems(payload: DictTypeAndASCodePayload): List<SysDictItemCacheItem>

    /**
     * 根据字典类型和原子服务编码，取得对应字典项的编码和名称(仅包括处于启用状态的)
     *
     * @param payload 字典类型和原子服务编码的载体
     * @return LinkedHashMap(编码，名称)，自然排序。查无结果返回空Map。
     * @throws IllegalArgumentException 参数校验不通过时
     * @author K
     * @since 1.0.0
     */
    fun getDictItemMap(payload: DictTypeAndASCodePayload): LinkedHashMap<String, String>

    /**
     * 根据字典类型和原子服务编码的载体列表，取得对应字典项信息(仅包括处于启用状态的)
     *
     * @param payloads 字典类型和原子服务编码的载体列表
     * @return Map(Pair(模块，字典类型)，List(字典项信息对象))
     * @throws IllegalArgumentException 参数校验不通过时
     * @author K
     * @since 1.0.0
     */
    fun batchGetDictItems(payloads: List<DictTypeAndASCodePayload>): Map<Pair<String, String>, List<SysDictItemCacheItem>>

    /**
     * 根据字典类型和原子服务编码的载体列表，取得对应字典项的编码和名称(仅包括处于启用状态的)
     *
     * @param payloads 字典类型和原子服务编码的载体列表
     * @return Map(Pair(模块，字典类型)，LinkedHashMap(编码，名称))
     * @throws IllegalArgumentException 参数校验不通过时
     * @author K
     * @since 1.0.0
     */
    fun batchGetDictItemMap(payloads: List<DictTypeAndASCodePayload>): Map<Pair<String, String>, LinkedHashMap<String, String>>


    //endregion your codes 2

}