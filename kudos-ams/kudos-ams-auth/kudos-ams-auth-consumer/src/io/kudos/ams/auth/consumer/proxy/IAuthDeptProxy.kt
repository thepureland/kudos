package io.kudos.ams.auth.consumer.proxy

import io.kudos.ams.auth.common.api.IAuthDeptApi
import io.kudos.ams.auth.consumer.fallback.AuthDeptFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 部门客户端代理接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "auth-dept", fallback = AuthDeptFallback::class)
interface IAuthDeptProxy : IAuthDeptApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
