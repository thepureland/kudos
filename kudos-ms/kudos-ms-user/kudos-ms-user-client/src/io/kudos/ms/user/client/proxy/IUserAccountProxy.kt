package io.kudos.ms.user.client.proxy

import io.kudos.ms.user.client.fallback.UserAccountFallback
import io.kudos.ms.user.common.api.IUserAccountApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 用户客户端代理接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@FeignClient(name = "user-account", fallback = UserAccountFallback::class)
interface IUserAccountProxy : IUserAccountApi {



}
