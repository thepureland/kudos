package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysDataSourceApi
import io.kudos.ms.sys.common.vo.datasource.SysDataSourceCacheItem
import io.kudos.ms.sys.core.service.iservice.ISysDataSourceService
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * 数据源 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@Component
open class SysDataSourceApi : ISysDataSourceApi {
//endregion your codes 1

    //region your codes 2

    @Resource
    protected lateinit var sysDataSourceService: ISysDataSourceService

    override fun getDataSource(
        tenantId: String,
        atomicServiceCode: String?
    ): SysDataSourceCacheItem? {
        return sysDataSourceService.getDataSource(tenantId, atomicServiceCode)
    }

    //endregion your codes 2

}