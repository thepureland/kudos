package io.kudos.ms.user.api.public.controller.passport

import io.kudos.base.security.BarcodeKit
import io.kudos.context.core.KudosContext
import io.kudos.ms.user.common.passport.CurrentUserKit
import io.kudos.ms.user.common.passport.enums.ChangePasswordResultEnum
import io.kudos.ms.user.common.passport.enums.PassportLoginStatusEnum
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.ms.user.common.passport.vo.request.ChangePasswordRequest
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.request.VerifyPasswordRequest
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import io.kudos.ms.user.common.passport.vo.response.UserInfoModel
import io.kudos.ms.user.core.passport.service.iservice.IPassportService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


/**
 * Passport public HTTP controller (accessed by end users).
 *
 * **Session model**: on successful login a `SessionUserPrincipal` is written to
 * `HttpSession[SESSION_KEY_USER]`, and `UserContextWebFilter` injects it into
 * `KudosContext.user` on subsequent requests. `CurrentUserKit` is the standard
 * entry point for reading the current user from controllers/services.
 *
 * **`@RequestParam userId` endpoints are still retained** (verifyPassword /
 * changePassword / logout / etc.): they support cross-service RPC calls (which
 * have no session context) and act as a fallback when no session scheme is
 * active. The gateway layer must ensure these public endpoints, when invoked
 * with a `userId`, cannot operate across users.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/public/user/passport")
class PassportPublicController(
    private val passportService: IPassportService,
) {

    @PostMapping("/login")
    fun login(@RequestBody @Valid req: PassportLoginRequest, request: HttpServletRequest): PassportLoginResult {
        val res = passportService.login(req)
        if (res.status == PassportLoginStatusEnum.SUCCESS) {
            val info = res.userInfo ?: return res
            // Write into HttpSession; UserContextWebFilter reads it back into KudosContext.user on subsequent requests
            val principal = SessionUserPrincipal(
                id = info.id,
                tenantId = info.tenantId,
                username = info.username,
            )
            request.session.setAttribute(KudosContext.SESSION_KEY_USER, principal)
        }
        return res
    }

    /**
     * Logout: writes the last-logout time for audit, and invalidates the session so
     * [UserContextWebFilter] can no longer resolve the user on subsequent requests.
     *
     * If [userId] is not provided, it is resolved from the current session, allowing
     * the frontend to "log myself out" without re-supplying the id.
     */
    @PostMapping("/logout")
    fun logout(
        @RequestParam(required = false) userId: String?,
        request: HttpServletRequest,
    ): Boolean {
        val effectiveUserId = userId ?: CurrentUserKit.currentUserIdOrNull() ?: return false
        val ok = passportService.logout(effectiveUserId)
        // Drop the session regardless of service-call outcome — the client wants to log out
        request.getSession(false)?.invalidate()
        return ok
    }

    /**
     * Returns a brief summary of the currently logged-in user; returns null if not logged in.
     * Commonly used by the frontend for "am I still logged in after a page refresh?" checks.
     */
    @GetMapping("/me")
    fun me(): UserInfoModel? {
        val p = CurrentUserKit.currentPrincipalOrNull() ?: return null
        // Only the 3 fields available in the session are returned; for the full profile, fetch from sysUserApi
        return UserInfoModel(
            id = p.id,
            username = p.username,
            tenantId = p.tenantId,
            orgId = null,
            accountTypeDictCode = null,
            defaultLocale = null,
            defaultTimezone = null,
            defaultCurrency = null,
            loginTime = java.time.LocalDateTime.now(),
        )
    }

    /** Verify the current user's login password (does not consume the error counter). Used for re-authentication before sensitive operations. */
    @PostMapping("/verifyPassword")
    fun verifyPassword(@RequestBody @Valid req: VerifyPasswordRequest): Boolean =
        passportService.verifyPassword(req)

    /** Verify the current user's security password (does not consume the error counter). */
    @PostMapping("/verifySecurityPassword")
    fun verifySecurityPassword(@RequestBody @Valid req: VerifyPasswordRequest): Boolean =
        passportService.verifySecurityPassword(req)

    /** User changes their own login password: verifies the old password first. */
    @PostMapping("/changePassword")
    fun changePassword(@RequestBody @Valid req: ChangePasswordRequest): ChangePasswordResultEnum =
        passportService.changePassword(req)

    /** User changes their own security password. */
    @PostMapping("/changeSecurityPassword")
    fun changeSecurityPassword(@RequestBody @Valid req: ChangePasswordRequest): ChangePasswordResultEnum =
        passportService.changeSecurityPassword(req)

    /**
     * Renders any short text (typically `otpauth://...`) as a PNG QR code.
     *
     * Usually paired with [io.kudos.ms.user.common.account.vo.response.AuthKeySetup.otpauthUrl]:
     * the frontend obtains the otpauth URL, then GETs this endpoint and displays the response as an `<img>`.
     */
    @GetMapping("/qrCode", produces = [MediaType.IMAGE_PNG_VALUE])
    fun qrCode(
        @RequestParam text: String,
        @RequestParam(required = false, defaultValue = "200") size: Int,
    ): ByteArray = BarcodeKit.qrcodePng(text, size = size)

}
