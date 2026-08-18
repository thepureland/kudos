package io.kudos.ms.user.api.internal.controller.contact

import io.kudos.ms.user.common.contact.api.IUserContactWayApi
import io.kudos.ms.user.core.contact.api.UserContactWayApi
import org.springframework.web.bind.annotation.RestController


/**
 * User contact way internal RPC controller. Paths are inherited from method-level annotations
 * on [IUserContactWayApi].
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class UserContactWayInternalController(
    private val userContactWayApi: UserContactWayApi,
) : IUserContactWayApi {

    override fun getActiveContactValuesByUserIds(
        userIds: Collection<String>,
        contactWayDictCode: String,
    ): Map<String, String> = userContactWayApi.getActiveContactValuesByUserIds(userIds, contactWayDictCode)

}
