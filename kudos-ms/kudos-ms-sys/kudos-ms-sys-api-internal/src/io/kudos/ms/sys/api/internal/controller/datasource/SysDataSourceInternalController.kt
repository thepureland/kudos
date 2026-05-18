package io.kudos.ms.sys.api.internal.controller.datasource

import io.kudos.ms.sys.common.datasource.api.ISysDataSourceApi
import io.kudos.ms.sys.common.datasource.vo.SysDataSourceCacheEntry
import io.kudos.ms.sys.core.datasource.api.SysDataSourceApi
import org.springframework.web.bind.annotation.RestController


/**
 * 数据源 内部 RPC 控制器。路径继承自 [ISysDataSourceApi] 方法级注解。
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
