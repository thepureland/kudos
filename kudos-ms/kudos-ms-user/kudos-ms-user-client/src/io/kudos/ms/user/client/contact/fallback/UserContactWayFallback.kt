package io.kudos.ms.user.client.contact.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.user.client.contact.proxy.IUserContactWayProxy


/**
 * User contact way fallback.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class UserContactWayFallback :
    AbstractHttpFallbackSupport("UserContactWayFallback"), IUserContactWayProxy {

    /**
     * Degraded read: returns an empty map, i.e. "no receiver has a contact of this type".
     *
     * Callers (the messaging dispatch listeners) already treat an absent user as "no contact" and record
     * them as undelivered, so a message is never silently dropped — it lands in the undelivered ledger
     * and can be retried once user is reachable again.
     */
    override fun getActiveContactValuesByUserIds(
        userIds: Collection<String>,
        contactWayDictCode: String,
    ): Map<String, String> {
        warnRead("getActiveContactValuesByUserIds", userIds.size, contactWayDictCode)
        return emptyMap()
    }

}
