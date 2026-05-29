package io.kudos.ability.web.guest.init.properties

import java.time.Duration

/**
 * Storage-side tunables for the guest-tracking module: which Redis to use, how long an active
 * visitor is "remembered", and what request data piggy-backs into the stored payload.
 *
 * Lifetime semantics:
 *  - [timeout] is the Redis TTL on each visitor key; every refresh request rolls it forward, so a
 *    visitor that keeps clicking stays "online" indefinitely, while one who walks away ages out.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class GuestRepository {

    /**
     * Name of the [io.kudos.ability.data.memdb.redis.RedisTemplates] entry used. Blank/null falls
     * back to the default template — apps with a dedicated session/data Redis can route guest
     * traffic onto that node without touching the rest of the stack.
     */
    var groupName: String? = null

    /** Key prefix for the visitor's hash. */
    var prefix: String = "guest-prefix"

    /**
     * Redis TTL on each visitor key. Apps that just want a "rolling 5-minute online" gauge keep
     * the default; longer windows produce a more inclusive count but cost more Redis memory.
     */
    var timeout: Duration = Duration.ofMinutes(5)

    /** Extra request data to ship along with the visitor key. See [GuestPayload]. */
    var payload: GuestPayload = GuestPayload()

    /**
     * Configurable "additional payload" for each visitor.
     *
     * The default impl stores a Redis hash whose fields come from two reflection-driven lists:
     *  - [paramNames] — HTTP query parameters by name, e.g. `["v"]` captures the campaign id from
     *    `?v=xyz`.
     *  - [clientInfos] — properties of the bound [io.kudos.context.core.ClientInfo], read via
     *    [io.kudos.base.bean.BeanKit.getProperty]. Names not present on [ClientInfo] resolve to
     *    blank rather than crash, so a typo doesn't break the request.
     *
     * Keeping both lists empty (default) stores no payload at all — the visitor key alone is
     * enough for a count-based dashboard.
     */
    class GuestPayload {
        var paramNames: List<String> = emptyList()
        var clientInfos: List<String> = emptyList()
    }
}
