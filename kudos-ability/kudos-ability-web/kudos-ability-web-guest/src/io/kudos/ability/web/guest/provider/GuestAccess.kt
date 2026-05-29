package io.kudos.ability.web.guest.provider

/**
 * Value object representing a single visitor.
 *
 * Three fields, each set at a distinct lifecycle stage:
 *  - [token] — the random AES-encrypted string round-tripped in the cookie. Allocated when no
 *    existing cookie is presented; on subsequent requests, the decrypted token from the inbound
 *    cookie populates this field.
 *  - [hash] — a stable, request-derived fingerprint (default: `MD5(User-Agent + IP)`). Used as
 *    the Redis hash key so revisits collapse onto the same record even when the token cookie is
 *    re-minted (e.g. cookie expired and the visitor returned later).
 *  - [payload] — caller-configurable extra data shipped alongside the hash key; see
 *    [io.kudos.ability.web.guest.init.properties.GuestRepository.GuestPayload].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class GuestAccess {
    var token: String? = null
    var hash: String? = null
    var payload: Map<String, String>? = null
}
