package io.kudos.ms.user.common.contact.api

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

/**
 * User contact way external API
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IUserContactWayApi {

    /**
     * Batch fetch the active contact value of one contact type for the given users.
     *
     * Used by messaging pipelines: before sending email / SMS, batch fetch email / phone by user id.
     * - `contactWayDictCode` values refer to SQL dictionary `contact_way`, e.g. `"201"` denotes email.
     * - When a user has several contact ways of the same type, the one with the smallest `priority` wins
     *   (null sorts last).
     * - Only records with `active = true` are considered, so disabled contact ways are never messaged.
     *
     * @param userIds user ids to look up
     * @param contactWayDictCode contact way dict code (SQL dictionary `contact_way`)
     * @return Map<userId, contactWayValue>; users without an available contact way of this type are absent
     * @author K
     * @since 1.0.0
     */
    @PostMapping("/api/internal/user/contactWay/getActiveContactValuesByUserIds")
    fun getActiveContactValuesByUserIds(
        @RequestBody userIds: Collection<String>,
        @RequestParam contactWayDictCode: String,
    ): Map<String, String>

}
