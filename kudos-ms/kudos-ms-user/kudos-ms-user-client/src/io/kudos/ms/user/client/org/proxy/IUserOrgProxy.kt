package io.kudos.ms.user.client.org.proxy

import io.kudos.ms.user.client.org.fallback.UserOrgFallback
import io.kudos.ms.user.common.org.api.IUserOrgApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * Organization client proxy interface.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@FeignClient(name = "user-org", fallback = UserOrgFallback::class)
interface IUserOrgProxy : IUserOrgApi {



}
