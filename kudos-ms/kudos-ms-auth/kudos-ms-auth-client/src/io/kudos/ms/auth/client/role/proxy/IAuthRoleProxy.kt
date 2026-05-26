package io.kudos.ms.auth.client.role.proxy

import io.kudos.ms.auth.client.role.fallback.AuthRoleFallback
import io.kudos.ms.auth.common.role.api.IAuthRoleApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * Client-side proxy interface for the role API.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@FeignClient(name = "auth-role", fallback = AuthRoleFallback::class)
interface IAuthRoleProxy : IAuthRoleApi {



}
