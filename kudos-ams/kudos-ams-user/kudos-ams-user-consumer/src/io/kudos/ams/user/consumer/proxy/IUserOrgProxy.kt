package io.kudos.ams.user.consumer.proxy

import io.kudos.ams.user.common.api.IUserOrgApi
import io.kudos.ams.user.consumer.fallback.UserOrgFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 机构客户端代理接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "user-org", fallback = UserOrgFallback::class)
interface IUserOrgProxy : IUserOrgApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
