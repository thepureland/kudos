package io.kudos.ms.sys.common.api

import io.kudos.ms.sys.common.vo.param.SysParamCacheItem


/**
 * 参数 对外API
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysParamApi {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据参数名称和原子服务编码，取得对应参数
     *
     * @param paramName 参数名称
     * @param atomicServiceCode 原子服务编码，缺省为 "default"
     * @return 参数信息缓存对象。查无结果返回null。
     * @author K
     * @since 1.0.0
     */
    fun getParam(
        paramName: String,
        atomicServiceCode: String = "default"
    ): SysParamCacheItem?

    //endregion your codes 2

}