package io.kudos.ms.sys.core.datasource.api

import io.kudos.ms.sys.common.datasource.api.ISysDataSourceApi
import io.kudos.ms.sys.common.datasource.vo.SysDataSourceCacheEntry
import io.kudos.ms.sys.core.datasource.service.iservice.ISysDataSourceService
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component


/**
 * Local implementation of the data source API
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Primary
@Component
open class SysDataSourceApi(
    private val sysDataSourceService: ISysDataSourceService,
) : ISysDataSourceApi {

    override fun getDataSourceFromCache(
        tenantId: String,
        atomicServiceCode: String?
    ): SysDataSourceCacheEntry? = sysDataSourceService.getDataSourceFromCache(tenantId, atomicServiceCode)
}
