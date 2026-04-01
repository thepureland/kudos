package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysDataSourceApi
import io.kudos.ms.sys.common.vo.datasource.SysDataSourceCacheEntry
import io.kudos.ms.sys.core.service.iservice.ISysDataSourceService
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
