package io.kudos.ms.user.client.protection.proxy

import io.kudos.ms.user.client.protection.fallback.UserAccountProtectionFallback
import io.kudos.ms.user.common.protection.api.IUserAccountProtectionApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 用户账号保护客户端代理接口
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@FeignClient(name = "user-account-protection", fallback = UserAccountProtectionFallback::class)
interface IUserAccountProtectionProxy : IUserAccountProtectionApi {



}
