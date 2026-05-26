package io.kudos.ms.sys.api.internal.controller.datasource

import io.kudos.ms.sys.common.datasource.api.ISysDataSourceApi
import io.kudos.ms.sys.common.datasource.vo.SysDataSourceCacheEntry
import io.kudos.ms.sys.core.datasource.api.SysDataSourceApi
import org.springframework.web.bind.annotation.RestController


/**
 * Data source internal RPC controller. Paths are inherited from method-level annotations on [ISysDataSourceApi].
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class SysDataSourceInternalController(
    private val sysDataSourceApi: SysDataSourceApi,
) : ISysDataSourceApi {

    override fun getDataSourceFromCache(tenantId: String, atomicServiceCode: String?): SysDataSourceCacheEntry? =
        sysDataSourceApi.getDataSourceFromCache(tenantId, atomicServiceCode)

}
