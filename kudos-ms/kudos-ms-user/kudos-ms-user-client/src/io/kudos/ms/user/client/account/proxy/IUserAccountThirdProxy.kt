package io.kudos.ms.user.client.account.proxy

import io.kudos.ms.user.client.account.fallback.UserAccountThirdFallback
import io.kudos.ms.user.common.account.api.IUserAccountThirdApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * Third-party user account client proxy interface.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@FeignClient(name = "user-account-third", fallback = UserAccountThirdFallback::class)
interface IUserAccountThirdProxy : IUserAccountThirdApi {



}
