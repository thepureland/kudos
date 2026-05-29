package io.kudos.ability.web.guest.provider

import jakarta.servlet.http.HttpServletRequest

/**
 * Per-request skip predicate. The filter walks every registered [IGuestAccessIgnore] bean; any
 * `true` short-circuits guest tracking for this request.
 *
 * Soul shipped a [GuestAccessAuthedIgnore] default that consults Spring Security's
 * `SecurityContextHolder` to skip already-authenticated users. kudos has no spring-security wiring
 * yet, so that default is intentionally not ported — apps that adopt spring-security can wire
 * their own bean with the same logic; the rest stay on a pure-bypass model.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
fun interface IGuestAccessIgnore {

    /** Return true to bypass guest tracking for this request. */
    fun ignore(request: HttpServletRequest): Boolean
}
