package io.kudos.ability.web.guest.provider

import io.kudos.ability.web.guest.init.properties.GuestProperties
import io.kudos.base.bean.BeanKit
import io.kudos.base.lang.string.RandomStringKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.security.CryptoKit
import io.kudos.context.core.ClientInfo
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.ObjectProvider
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Default [IGuestAccessService] implementation.
 *
 * Wire format on the cookie:
 *  1. AES-encrypt a UUID with [GuestCookieProperties.cipherKey] → opaque token.
 *  2. URL-encode the token (single value here, but the format also supports multi-segment values
 *    separated by `:` — preserved verbatim from soul for forward-compat with payloads carrying
 *    `token:meta:more`).
 *  3. Base64-encode the URL-encoded form; strip trailing `=` padding.
 *  4. Read back: append `=` back to a length-multiple-of-4, Base64-decode, split on `:`,
 *    URL-decode each segment, AES-decrypt the first one.
 *
 * The double encoding looks redundant but isn't:
 *  - URL-encode protects the `:` segment separator from showing up inside a single segment.
 *  - Base64 makes the resulting string cookie-attribute-safe (no `=`, no spaces) without losing
 *    its bidirectional reversibility.
 *
 * Ported from soul's `GuestAccessService` with two simplifications:
 *  - Soul threw `ServiceException(GuestAccessErrorCode.GUEST_ACCESS_TOKEN_INVALID)` on decrypt
 *    failure; kudos doesn't have the IErrorCode enum convention so we throw a plain
 *    [IllegalStateException]. The filter catches and logs either way.
 *  - The [HttpServletRequest] IP extraction borrows kudos's existing
 *    [io.kudos.ability.web.springmvc.support.getRemoteIp] extension instead of duplicating the
 *    x-forwarded-for fan-out logic.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class GuestAccessService(
    private val properties: GuestProperties,
    private val uniqueKey: IGuestAccessUniqueKey,
    /** Lazy `ObjectProvider` so apps with zero registered ignore beans still wire cleanly. */
    private val ignores: ObjectProvider<IGuestAccessIgnore>,
) : IGuestAccessService {

    private val log = LogFactory.getLog(this::class)

    override fun isEnabled(): Boolean = properties.enabled

    override fun isExclude(request: HttpServletRequest): Boolean =
        ignores.stream().anyMatch { it.ignore(request) }

    override fun fetchGuestToken(request: HttpServletRequest): GuestAccess? {
        val initCookie = request.cookies?.firstOrNull { it.name == properties.cookie.name } ?: return null
        val value = initCookie.value ?: return null
        val cookieTokens = decodeCookie(value) ?: return null
        if (cookieTokens.isEmpty()) return null
        val token = decryptToken(cookieTokens[0]) ?: return null
        return GuestAccess().apply { this.token = token }
    }

    override fun hash(request: HttpServletRequest, guestAccess: GuestAccess) {
        guestAccess.hash = uniqueKey.gen(request)
        guestAccess.payload = loadPayload(request)
    }

    override fun genToken(request: HttpServletRequest, response: HttpServletResponse): GuestAccess {
        val token = CryptoKit.aesEncrypt(RandomStringKit.uuid(), properties.cookie.cipherKey)
        setCookie(arrayOf(token), response)
        return GuestAccess().apply { this.token = token }
    }

    private fun decryptToken(token: String): String? {
        if (token.isBlank()) return null
        // tryAesDecrypt surfaces failure via Result rather than [CryptoKit.aesDecrypt]'s
        // "return empty string on failure" shim — we MUST distinguish "decrypted to empty" from
        // "decryption failed", because the latter is what indicates a tampered cookie.
        return CryptoKit.tryAesDecrypt(token, properties.cookie.cipherKey)
            .map { it.takeIf { plain -> plain.isNotBlank() } }
            .getOrElse {
                // Forged / wrong-key cookie. Returning null routes through the filter's
                // "first-visit" branch so the attacker just gets a freshly-minted cookie — same
                // outcome as presenting no cookie at all. Bad token is never trusted downstream.
                log.warn("Guest access token failed to decrypt; treating request as a new visitor (reason={0})", it.message)
                null
            }
    }

    private fun loadPayload(request: HttpServletRequest): Map<String, String> {
        val payload = LinkedHashMap<String, String>()
        val cfg = properties.repository.payload
        for (name in cfg.paramNames) {
            payload[name] = request.getParameter(name).orEmpty()
        }
        if (cfg.clientInfos.isNotEmpty()) {
            val info = buildClientInfo(request)
            for (field in cfg.clientInfos) {
                val v = try {
                    BeanKit.getProperty(info, field)
                } catch (e: Exception) {
                    // Field names that don't exist on ClientInfo resolve to blank rather than
                    // crash the request — the yml is user-edited so typos must not be fatal.
                    log.debug(
                        "Guest payload client-info field [{0}] not resolvable; storing empty (reason={1})",
                        field, e.message,
                    )
                    null
                }
                payload[field] = v?.toString().orEmpty()
            }
        }
        return payload
    }

    private fun buildClientInfo(request: HttpServletRequest): ClientInfo =
        ClientInfo.Builder()
            .domain(request.serverName)
            .url(request.requestURL?.toString())
            .requestReferer(request.getHeader("Referer"))
            .requestType(request.method)
            .build()

    private fun setCookie(tokens: Array<String>, response: HttpServletResponse) {
        val cookieValue = encodeCookie(tokens)
        val cookie = Cookie(properties.cookie.name, cookieValue).apply {
            maxAge = properties.cookie.maxAge.toSeconds().toInt()
            path = properties.cookie.path
            properties.cookie.domain?.takeIf { it.isNotBlank() }?.let { this.domain = it }
            isHttpOnly = properties.cookie.httpOnly
        }
        response.addCookie(cookie)
    }

    internal fun encodeCookie(cookieTokens: Array<String>): String {
        val joined = cookieTokens.joinToString(DELIMITER) {
            URLEncoder.encode(it, StandardCharsets.UTF_8)
        }
        return Base64.getEncoder().encodeToString(joined.toByteArray()).trimEnd('=')
    }

    internal fun decodeCookie(cookieValue: String): Array<String>? {
        val padded = cookieValue + "=".repeat((4 - cookieValue.length % 4) % 4)
        val decoded = try {
            String(Base64.getDecoder().decode(padded))
        } catch (e: IllegalArgumentException) {
            log.warn("Cookie token was not Base64 encoded; value was [{0}]", cookieValue)
            return null
        }
        return decoded.split(DELIMITER)
            .map { URLDecoder.decode(it, StandardCharsets.UTF_8) }
            .toTypedArray()
    }

    companion object {
        private const val DELIMITER = ":"
    }
}
