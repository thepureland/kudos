package io.kudos.ams.user.client.proxy

import io.kudos.ams.user.common.api.IUserAccountThirdApi
import io.kudos.ams.user.client.fallback.UserAccountThirdFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 用户第三方账号客户端代理接口
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "user-account-third", fallback = UserAccountThirdFallback::class)
interface IUserAccountThirdProxy : IUserAccountThirdApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
