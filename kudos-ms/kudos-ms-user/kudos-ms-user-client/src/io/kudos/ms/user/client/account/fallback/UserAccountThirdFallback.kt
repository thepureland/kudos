package io.kudos.ms.user.client.account.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.user.client.account.proxy.IUserAccountThirdProxy
import org.springframework.stereotype.Component


/**
 * 用户第三方账号 Feign 容错降级实现。`IUserAccountThirdApi` 当前未定义对外方法，
 * 保留类作为 `@FeignClient(fallback=...)` 的合法目标。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class UserAccountThirdFallback :
    AbstractFeignFallbackSupport("UserAccountThirdFallback"), IUserAccountThirdProxy
