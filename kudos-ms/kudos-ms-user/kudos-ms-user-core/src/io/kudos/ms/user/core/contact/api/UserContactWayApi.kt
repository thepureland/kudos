package io.kudos.ms.user.core.contact.api

import io.kudos.ms.user.common.contact.api.IUserContactWayApi
import io.kudos.ms.user.core.contact.service.iservice.IUserContactWayService
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service


/**
 * User contact way API local implementation
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Primary
@Service
open class UserContactWayApi(
    private val userContactWayService: IUserContactWayService,
) : IUserContactWayApi {

    override fun getActiveContactValuesByUserIds(
        userIds: Collection<String>,
        contactWayDictCode: String,
    ): Map<String, String> = userContactWayService.getActiveContactValuesByUserIds(userIds, contactWayDictCode)

}
