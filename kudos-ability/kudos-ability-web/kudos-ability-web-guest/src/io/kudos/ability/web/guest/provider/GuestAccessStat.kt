package io.kudos.ability.web.guest.provider

/**
 * Aggregate snapshot for "how many active visitors are there right now".
 *
 * Single field for now — [count] is the number of live keys under the configured prefix at scan
 * time. Soul's class shipped a per-terminal breakdown field, but the only store impl never
 * populated it; kept the field off the kudos port until a real backend that emits it exists.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class GuestAccessStat {
    var count: Int = 0
}
