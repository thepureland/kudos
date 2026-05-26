package io.kudos.ms.user.core.contact.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.user.core.contact.model.po.UserContactWay


/**
 * User contact way service interface
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IUserContactWayService : IBaseCrudService<String, UserContactWay> {

    /**
     * Query the "active and highest-priority" value of the specified contact way type for multiple users.
     *
     * Used in messaging pipelines: before sending email / SMS, batch fetch email / phone by user id.
     * - `contactWayDictCode` values refer to SQL dictionary `contact_way`, e.g. `"201"` denotes email.
     * - When a user has multiple contact ways of the same type, take the first by `priority ASC` (smaller priority takes precedence; null sorts last).
     * - Only records with `active = true` are considered, to avoid sending messages to disabled contact ways.
     *
     * @return Map<userId, contactWayValue>; users without an available contact way of this type are absent from the map
     */
    fun getActiveContactValuesByUserIds(
        userIds: Collection<String>,
        contactWayDictCode: String,
    ): Map<String, String>

}
