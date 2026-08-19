package io.kudos.ms.sys.client.datasource.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.sys.client.datasource.proxy.ISysDataSourceProxy
import io.kudos.ms.sys.common.datasource.vo.SysDataSourceCacheEntry


/**
 * Data source fallback implementation.
 *
 * @author K
 * @since 1.0.0
 */
open class SysDataSourceFallback : AbstractHttpFallbackSupport("SysDataSourceFallback"), ISysDataSourceProxy {

    override fun getDataSourceFromCache(
        tenantId: String,
        atomicServiceCode: String?,
    ): SysDataSourceCacheEntry? {
        warnRead("getDataSourceFromCache", tenantId, atomicServiceCode)
        return null
    }
}
