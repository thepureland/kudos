package io.kudos.ms.sys.common.api

import io.kudos.ms.sys.common.vo.datasource.SysDataSourceCacheItem


/**
 * 数据源 对外API
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysDataSourceApi {
//endregion your codes 1

    //region your codes 2

    /**
     * 返回指定租户ID和原子服务编码的数据源
     *
     * @param tenantId 租户ID
     * @param atomicServiceCode 原子服务编码，缺省为null，为空时请自行确保tenantId对应数据源的惟一性，否则将随机返回一条
     * @return SysDataSourceCacheItem，找不到时返回null
     * @author K
     * @since 1.0.0
     */
    fun getDataSource(
        tenantId: String,
        atomicServiceCode: String? = null
    ): SysDataSourceCacheItem?

    //endregion your codes 2

}