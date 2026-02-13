package io.kudos.ms.sys.common.api

import io.kudos.ms.sys.common.vo.datasource.SysDataSourceCacheItem
import io.kudos.ms.sys.common.vo.datasource.TenantIdAndASCodePayload


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
     * @param payload 租户ID和原子服务编码载体
     * @return SysDataSourceCacheItem
     * @author K
     * @since 1.0.0
     */
    fun getDataSource(payload: TenantIdAndASCodePayload): SysDataSourceCacheItem?

    //endregion your codes 2

}