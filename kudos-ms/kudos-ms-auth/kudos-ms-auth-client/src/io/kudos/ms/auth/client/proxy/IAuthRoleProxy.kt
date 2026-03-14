package io.kudos.ms.auth.client.proxy

import io.kudos.ms.auth.client.fallback.AuthRoleFallback
import io.kudos.ms.auth.common.api.IAuthRoleApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 角色客户端代理接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@FeignClient(name = "auth-role", fallback = AuthRoleFallback::class)
interface IAuthRoleProxy : IAuthRoleApi {



}
