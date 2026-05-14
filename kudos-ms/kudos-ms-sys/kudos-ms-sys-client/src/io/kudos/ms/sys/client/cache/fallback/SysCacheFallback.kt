package io.kudos.ms.sys.client.cache.fallback

import io.kudos.ms.sys.client.cache.proxy.ISysCacheProxy
import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import org.springframework.stereotype.Component


/**
 * 缓存 Feign 容错降级实现。`ISysCacheApi` 当前未定义对外方法，保留类作为 `@FeignClient(fallback=...)` 的合法目标。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysCacheFallback : SysClientFallbackSupport("SysCacheFallback"), ISysCacheProxy
