package io.kudos.ms.sys.client.datasource.fallback

import io.kudos.ms.sys.client.datasource.proxy.ISysDataSourceProxy
import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import io.kudos.ms.sys.common.datasource.vo.SysDataSourceCacheEntry
import org.springframework.stereotype.Component


/**
 * Data source Feign fallback implementation.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysDataSourceFallback : SysClientFallbackSupport("SysDataSourceFallback"), ISysDataSourceProxy {

    override fun getDataSourceFromCache(
        tenantId: String,
        atomicServiceCode: String?,
    ): SysDataSourceCacheEntry? {
        warnRead("getDataSourceFromCache", tenantId, atomicServiceCode)
        return null
    }
}
