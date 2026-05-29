package io.kudos.ability.web.guest.provider

import io.kudos.ability.web.guest.init.properties.GuestProperties
import io.kudos.ability.web.springmvc.support.getRemoteIp
import io.kudos.base.security.DigestKit
import jakarta.servlet.http.HttpServletRequest

/**
 * Default [IGuestAccessUniqueKey]: `MD5(User-Agent + remoteIp, cipherKey)`.
 *
 * The cookie's [GuestProperties.cookie.cipherKey][io.kudos.ability.web.guest.init.properties.GuestCookieProperties.cipherKey]
 * doubles as the MD5 salt — so even if an attacker enumerates UA+IP, they can't reproduce stored
 * hashes without also stealing the deployment's cipherKey.
 *
 * Fingerprint quality caveats:
 *  - Mobile networks NAT thousands of users behind one egress IP; carrier-grade visitors will
 *    collapse onto the same key if their UA strings also match (older / vendor-skinned Android
 *    builds in particular).
 *  - Privacy-focused browsers randomize the UA; their key changes every request and the count
 *    inflates.
 *
 * Apps that need a tighter or looser fingerprint declare their own [IGuestAccessUniqueKey] bean
 * — soul never shipped a "device id" alternative; kudos leaves that as an app concern.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class GuestAccessUniqueKey(private val properties: GuestProperties) : IGuestAccessUniqueKey {

    override fun gen(request: HttpServletRequest): String {
        val agent = request.getHeader("User-Agent") ?: ""
        val ip = request.getRemoteIp()
        return DigestKit.getMD5(agent + ip, properties.cookie.cipherKey)
    }
}
