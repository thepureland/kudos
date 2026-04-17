package io.kudos.ms.sys.core.datasource.api

import io.kudos.ms.sys.common.datasource.api.ISysDataSourceApi
import io.kudos.ms.sys.common.datasource.vo.SysDataSourceCacheEntry
import io.kudos.ms.sys.core.datasource.service.iservice.ISysDataSourceService
import org.springframework.stereotype.Component


/**
 * 数据源 API本地实现
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class SysDataSourceApi(
    private val sysDataSourceService: ISysDataSourceService,
) : ISysDataSourceApi {

    override fun getDataSourceFromCache(
        tenantId: String,
        atomicServiceCode: String?
    ): SysDataSourceCacheEntry? = sysDataSourceService.getDataSourceFromCache(tenantId, atomicServiceCode)
}
