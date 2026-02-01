package io.kudos.ams.user.client.proxy

import io.kudos.ams.user.common.api.IUserAccountProtectionApi
import io.kudos.ams.user.client.fallback.UserAccountProtectionFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 用户账号保护客户端代理接口
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "user-account-protection", fallback = UserAccountProtectionFallback::class)
interface IUserAccountProtectionProxy : IUserAccountProtectionApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
