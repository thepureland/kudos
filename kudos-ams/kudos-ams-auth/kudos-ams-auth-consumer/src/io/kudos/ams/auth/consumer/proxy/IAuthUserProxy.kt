package io.kudos.ams.auth.consumer.proxy

import io.kudos.ams.auth.common.api.IAuthUserApi
import io.kudos.ams.auth.consumer.fallback.AuthUserFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 用户客户端代理接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "auth-user", fallback = AuthUserFallback::class)
interface IAuthUserProxy : IAuthUserApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
