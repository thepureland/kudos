package io.kudos.ms.auth.api.public.controller.authentication

import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.core.authentication.lifecycle.service.iservice.IAuthenticationLifecycleService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Current-user logout operations spanning browser sessions and API token families. */
@RestController
@RequestMapping("/api/public/auth")
open class AuthenticationLogoutPublicController(
    private val lifecycleService: IAuthenticationLifecycleService,
) {

    /** Invalidates every device and token family owned by the current tenant user. */
    @PostMapping("/logout-all")
    open fun logoutAll(request: HttpServletRequest): Boolean {
        val httpSession = request.getSession(false) ?: return false
        val principal = httpSession.getAttribute(KudosContext.SESSION_KEY_USER) as? SessionUserPrincipal
            ?: return false
        lifecycleService.invalidateAll(principal.tenantId, principal.id, USER_LOGOUT_ALL)
        httpSession.invalidate()
        return true
    }

    private companion object {
        const val USER_LOGOUT_ALL = "USER_LOGOUT_ALL"
    }
}
