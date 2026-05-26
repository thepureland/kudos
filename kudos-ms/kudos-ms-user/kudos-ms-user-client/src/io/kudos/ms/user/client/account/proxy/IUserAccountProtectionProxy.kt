package io.kudos.ms.user.client.account.proxy

import io.kudos.ms.user.client.account.fallback.UserAccountProtectionFallback
import io.kudos.ms.user.common.account.api.IUserAccountProtectionApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * User account protection client proxy interface.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@FeignClient(name = "user-account-protection", fallback = UserAccountProtectionFallback::class)
interface IUserAccountProtectionProxy : IUserAccountProtectionApi {



}
