package io.kudos.ms.sys.common.datasource.api

import io.kudos.ms.sys.common.datasource.vo.SysDataSourceCacheEntry


/**
 * 数据源 对外API
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysDataSourceApi {

    /**
     * 返回指定租户ID和原子服务编码的数据源（从缓存）
     *
     * @param tenantId 租户ID
     * @param atomicServiceCode 原子服务编码，缺省为 null；为空时请自行确保 tenantId 对应数据源的惟一性，否则将随机返回一条
     * @return SysDataSourceCacheEntry，找不到时返回 null
     */
    fun getDataSourceFromCache(
        tenantId: String,
        atomicServiceCode: String? = null
    ): SysDataSourceCacheEntry?


}
