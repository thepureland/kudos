package io.kudos.ms.user.api.public.controller.passport

import io.kudos.base.security.BarcodeKit
import io.kudos.base.net.IpKit
import io.kudos.ability.web.springmvc.support.getBrowserInfo
import io.kudos.ability.web.springmvc.support.getClientTerminal
import io.kudos.ability.web.springmvc.support.getOsInfo
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
 * Public self-service operations always bind the request user id to the current session principal.
 * Trusted cross-service calls belong on an internal API and must not reuse this public boundary.
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
        // Never trust client-supplied audit metadata on the public boundary. Forwarded headers are
        // deliberately not parsed here; a trusted proxy/container should normalize remoteAddr.
        val browser = request.getBrowserInfo().asClientDescription()
        val os = request.getOsInfo().asClientDescription()
        val observedIp = IpKit.ipv4StringToLong(request.remoteAddr).takeIf { it >= 0 }
        val observedReq = req.copy(
            loginIp = observedIp,
            loginDevice = request.getClientTerminal(),
            loginBrowser = browser,
            loginOs = os,
            userAgent = request.getHeader("User-Agent"),
        )
        val res = passportService.login(observedReq)
        if (res.status == PassportLoginStatusEnum.SUCCESS) {
            val info = res.userInfo ?: return res
            // Write into HttpSession; UserContextWebFilter reads it back into KudosContext.user on subsequent requests
            val principal = SessionUserPrincipal(
                id = info.id,
                tenantId = info.tenantId,
                username = info.username,
            )
            val session = request.getSession(true)
            // Rotate the id after credential verification to prevent session fixation.
            request.changeSessionId()
            session.setAttribute(KudosContext.SESSION_KEY_USER, principal)
        }
        return res.toPublicLoginResult()
    }

    /**
     * Logout: writes the last-logout time for audit, and invalidates the session so
     * [UserContextWebFilter] can no longer resolve the user on subsequent requests.
     *
     * [userId] remains optional for wire compatibility, but when supplied it must match the
     * current principal. It can never select another user's account.
     */
    @PostMapping("/logout")
    fun logout(
        @RequestParam(required = false) userId: String?,
        request: HttpServletRequest,
    ): Boolean {
        val currentUserId = CurrentUserKit.currentUserIdOrNull() ?: return false
        if (userId != null && userId != currentUserId) return false
        val ok = passportService.logout(currentUserId)
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
    fun verifyPassword(@RequestBody @Valid req: VerifyPasswordRequest): Boolean {
        if (!isCurrentUser(req.userId)) return false
        return passportService.verifyPassword(req)
    }

    /** Verify the current user's security password (does not consume the error counter). */
    @PostMapping("/verifySecurityPassword")
    fun verifySecurityPassword(@RequestBody @Valid req: VerifyPasswordRequest): Boolean {
        if (!isCurrentUser(req.userId)) return false
        return passportService.verifySecurityPassword(req)
    }

    /** User changes their own login password: verifies the old password first. */
    @PostMapping("/changePassword")
    fun changePassword(@RequestBody @Valid req: ChangePasswordRequest): ChangePasswordResultEnum {
        if (!isCurrentUser(req.userId)) return ChangePasswordResultEnum.USER_NOT_FOUND
        return passportService.changePassword(req)
    }

    /** User changes their own security password. */
    @PostMapping("/changeSecurityPassword")
    fun changeSecurityPassword(@RequestBody @Valid req: ChangePasswordRequest): ChangePasswordResultEnum {
        if (!isCurrentUser(req.userId)) return ChangePasswordResultEnum.USER_NOT_FOUND
        return passportService.changeSecurityPassword(req)
    }

    /**
     * Renders any short text (typically `otpauth://...`) as a PNG QR code.
     *
     * Usually paired with [io.kudos.ms.user.common.account.vo.response.AuthKeySetup.otpauthUrl]:
     * the frontend obtains the otpauth URL, then GETs this endpoint and displays the response as an `<img>`.
     *
     * [size] is clamped to [[MIN_QR_SIZE], [MAX_QR_SIZE]]: this is an unauthenticated endpoint, and an
     * unbounded side length would let a single request allocate a huge BufferedImage (memory-exhaustion DoS).
     */
    @GetMapping("/qrCode", produces = [MediaType.IMAGE_PNG_VALUE])
    fun qrCode(
        @RequestParam text: String,
        @RequestParam(required = false, defaultValue = "200") size: Int,
    ): ByteArray = BarcodeKit.qrcodePng(text, size = size.coerceIn(MIN_QR_SIZE, MAX_QR_SIZE))

    companion object {
        /** Minimum QR code side length in pixels (smaller is unscannable). */
        private const val MIN_QR_SIZE = 64

        /** Maximum QR code side length in pixels (caps per-request image memory on this public endpoint). */
        private const val MAX_QR_SIZE = 1024
    }

    private fun isCurrentUser(userId: String): Boolean = CurrentUserKit.currentUserIdOrNull() == userId

    private fun Pair<String, String>.asClientDescription(): String =
        if (second == "unknown") first else "$first $second"

    /**
     * Keep the detailed result inside the Passport domain for audit, but do not disclose whether
     * the username exists or whether its account is disabled, locked, or administratively frozen.
     */
    private fun PassportLoginResult.toPublicLoginResult(): PassportLoginResult = when (status) {
        PassportLoginStatusEnum.USER_NOT_FOUND,
        PassportLoginStatusEnum.WRONG_PASSWORD,
        PassportLoginStatusEnum.INACTIVE,
        PassportLoginStatusEnum.LOCKED,
        PassportLoginStatusEnum.ACCOUNT_FROZEN -> PassportLoginResult.invalidCredentials()

        else -> this
    }

}
