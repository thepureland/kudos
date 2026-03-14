package io.kudos.ms.sys.client.proxy

import io.kudos.ms.sys.client.fallback.SysDataSourceFallback
import io.kudos.ms.sys.common.api.ISysDataSourceApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 数据源客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-datasource", fallback = SysDataSourceFallback::class)
interface ISysDataSourceProxy : ISysDataSourceApi {



}