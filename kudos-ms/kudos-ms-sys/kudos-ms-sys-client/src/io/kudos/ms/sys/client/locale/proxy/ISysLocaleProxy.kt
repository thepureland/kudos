package io.kudos.ms.sys.client.locale.proxy

import io.kudos.ms.sys.client.locale.fallback.SysLocaleFallback
import io.kudos.ms.sys.common.locale.api.ISysLocaleApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 语言/区域字典客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-locale", fallback = SysLocaleFallback::class)
interface ISysLocaleProxy : ISysLocaleApi
