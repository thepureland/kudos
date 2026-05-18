package io.kudos.ms.sys.client.outline.proxy

import io.kudos.ms.sys.client.outline.fallback.SysOutLineFallback
import io.kudos.ms.sys.common.outline.api.ISysOutLineApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 出网白名单客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-out-line", fallback = SysOutLineFallback::class)
interface ISysOutLineProxy : ISysOutLineApi
