package io.kudos.ms.sys.client.dict.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.sys.client.dict.proxy.ISysDictItemProxy
import org.springframework.stereotype.Component


/**
 * Dictionary item Feign fallback implementation. `ISysDictItemApi` currently defines no public methods; the class is kept as a valid `@FeignClient(fallback=...)` target.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysDictItemFallback : AbstractFeignFallbackSupport("SysDictItemFallback"), ISysDictItemProxy
