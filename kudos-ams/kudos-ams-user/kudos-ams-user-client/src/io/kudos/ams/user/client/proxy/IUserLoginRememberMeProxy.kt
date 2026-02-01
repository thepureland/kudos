package io.kudos.ams.user.client.proxy

import io.kudos.ams.user.common.api.IUserLoginRememberMeApi
import io.kudos.ams.user.client.fallback.UserLoginRememberMeFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 记住我登录客户端代理接口
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "user-login-remember-me", fallback = UserLoginRememberMeFallback::class)
interface IUserLoginRememberMeProxy : IUserLoginRememberMeApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
