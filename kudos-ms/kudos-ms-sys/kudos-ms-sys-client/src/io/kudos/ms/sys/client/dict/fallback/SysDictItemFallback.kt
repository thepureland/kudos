package io.kudos.ms.sys.client.dict.fallback

import io.kudos.ms.sys.client.dict.proxy.ISysDictItemProxy
import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import org.springframework.stereotype.Component


/**
 * 字典项 Feign 容错降级实现。`ISysDictItemApi` 当前未定义任何对外方法，保留类作为 `@FeignClient(fallback=...)` 的合法目标。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysDictItemFallback : SysClientFallbackSupport("SysDictItemFallback"), ISysDictItemProxy
