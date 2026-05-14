package io.kudos.ms.user.client.login.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.user.client.login.proxy.IUserLoginRememberMeProxy
import org.springframework.stereotype.Component


/**
 * 记住我登录 Feign 容错降级实现。`IUserLoginRememberMeApi` 当前未定义对外方法，
 * 保留类作为 `@FeignClient(fallback=...)` 的合法目标。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class UserLoginRememberMeFallback :
    AbstractFeignFallbackSupport("UserLoginRememberMeFallback"), IUserLoginRememberMeProxy
