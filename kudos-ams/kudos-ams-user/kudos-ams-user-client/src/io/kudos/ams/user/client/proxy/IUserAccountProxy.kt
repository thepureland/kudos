package io.kudos.ams.user.client.proxy

import io.kudos.ams.user.common.api.IUserAccountApi
import io.kudos.ams.user.client.fallback.UserAccountFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 用户客户端代理接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "user-account", fallback = UserAccountFallback::class)
interface IUserAccountProxy : IUserAccountApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
