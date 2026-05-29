package io.kudos.ability.web.guest.init.properties

import java.time.Duration

/**
 * Cookie tunables for the guest-tracking filter.
 *
 * Drives how the AES-encrypted visitor token is emitted on the first response and how it is read
 * back on subsequent requests. The fields mirror the JEE Cookie attributes so the response cookie
 * can be set without an extra translation step.
 *
 * Soul parented its `GuestAccessCookieProperties` on a shared `CookieProperty` base class living in
 * `soul-ability-web-common`. The kudos web-common module is currently an empty placeholder, so the
 * cookie shape is inlined here. When kudos eventually grows a shared cookie base, fields should
 * migrate up.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class GuestCookieProperties {

    /** Cookie name used to ship the AES-encrypted token. Soul default: `gi`. */
    var name: String = "gi"

    /** Cookie path scope; usually `/` so every endpoint sees the token. */
    var path: String = "/"

    /** Optional domain attribute (apex domain when sharing the cookie across subdomains). */
    var domain: String? = null

    /**
     * Cookie lifetime. Aligns with how long a visitor token stays valid in the client; the
     * server-side Redis TTL is configured separately on
     * [GuestRepository.timeout] (it controls "presence" while this controls "remembrance").
     */
    var maxAge: Duration = Duration.ofDays(14)

    /** Marks the cookie HttpOnly so JS cannot read it. Default true; only flip for debugging. */
    var httpOnly: Boolean = true

    /**
     * `SameSite` attribute. `lax` mirrors soul's default. Set to `none` only when the cookie has to
     * cross-origins — also requires `Secure` (the filter does not set Secure today; flip
     * [httpOnly]+TLS at the reverse-proxy layer).
     */
    var sameSite: String = "lax"

    /**
     * AES key used to encrypt/decrypt the in-cookie token. Apps **must** override this in
     * production yml — the module ships with the same default soul shipped which is intended for
     * local development only.
     */
    var cipherKey: String = "d2tlLzY5WjZ0VUlRaE44RitZZU9nQVp1aEp0alhEMVc="
}
