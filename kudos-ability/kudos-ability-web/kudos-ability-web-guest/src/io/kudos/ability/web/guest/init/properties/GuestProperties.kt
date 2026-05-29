package io.kudos.ability.web.guest.init.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration root for the kudos-ability-web-guest module.
 *
 * Bound from `kudos.ability.web.guest.*` in `application.yml`. The default `enabled=false` matches
 * soul — a module that silently fires a servlet filter on every request is a footgun, so guest
 * tracking must be opted in explicitly.
 *
 * Ported from soul's `GuestAccessProperties` (renamed from `guest-access` to `guest` on the kudos
 * side — the module is already called `web-guest`, so the `-access` suffix was redundant).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "kudos.ability.web.guest")
class GuestProperties {

    /**
     * Master switch. Default false — the filter short-circuits before any cookie / Redis work so
     * apps that depend on the module but haven't enabled it pay only the no-op `doFilter` cost.
     */
    var enabled: Boolean = false

    /** Cookie attributes for the AES-encrypted visitor token. */
    var cookie: GuestCookieProperties = GuestCookieProperties()

    /** Where + how the visitor record is stored. */
    var repository: GuestRepository = GuestRepository()
}
