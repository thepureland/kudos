package io.kudos.ms.user.client.account.proxy

import io.kudos.ms.user.client.account.fallback.UserAccountFallback
import io.kudos.ms.user.common.account.api.IUserAccountApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * User account client proxy interface.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@FeignClient(name = "user-account", fallback = UserAccountFallback::class)
interface IUserAccountProxy : IUserAccountApi {



}
