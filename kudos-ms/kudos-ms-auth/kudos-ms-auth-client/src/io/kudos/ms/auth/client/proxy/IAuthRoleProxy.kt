package io.kudos.ms.auth.client.proxy

import io.kudos.ms.auth.common.api.IAuthRoleApi
import io.kudos.ms.auth.client.fallback.AuthRoleFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 角色客户端代理接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "auth-role", fallback = AuthRoleFallback::class)
interface IAuthRoleProxy : IAuthRoleApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
