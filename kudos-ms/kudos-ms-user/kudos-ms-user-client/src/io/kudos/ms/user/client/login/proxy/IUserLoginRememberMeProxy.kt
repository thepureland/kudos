package io.kudos.ms.user.client.login.proxy

import io.kudos.ms.user.client.login.fallback.UserLoginRememberMeFallback
import io.kudos.ms.user.common.login.api.IUserLoginRememberMeApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * Remember-me login client proxy interface.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@FeignClient(name = "user-login-remember-me", fallback = UserLoginRememberMeFallback::class)
interface IUserLoginRememberMeProxy : IUserLoginRememberMeApi {



}
