package io.kudos.ms.user.api.admin.controller.account

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.user.common.account.vo.request.UserAccountFormCreate
import io.kudos.ms.user.common.account.vo.request.UserAccountFormUpdate
import io.kudos.ms.user.common.account.vo.request.UserAccountQuery
import io.kudos.ms.user.common.account.vo.response.AuthKeySetup
import io.kudos.ms.user.common.account.vo.response.UserAccountDetail
import io.kudos.ms.user.common.account.vo.response.UserAccountEdit
import io.kudos.ms.user.common.account.vo.response.UserAccountRow
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime


/**
 * User account admin controller.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/user/account")
class UserAccountAdminController :
    BaseCrudController<String, IUserAccountService, UserAccountQuery, UserAccountRow, UserAccountDetail, UserAccountEdit, UserAccountFormCreate, UserAccountFormUpdate>() {

    /** Update active status. */
    @PutMapping("/updateActive")
    fun updateActive(@RequestParam id: String, @RequestParam active: Boolean): Boolean =
        service.updateActive(id, active)

    /** Reset login password (admin operation). */
    @PostMapping("/resetPassword")
    fun resetPassword(@RequestParam id: String, @RequestParam newPassword: String): Boolean =
        service.resetPassword(id, newPassword)

    /** Reset security password (admin operation). */
    @PostMapping("/resetSecurityPassword")
    fun resetSecurityPassword(@RequestParam id: String, @RequestParam newPassword: String): Boolean =
        service.resetSecurityPassword(id, newPassword)

    /**
     * Reset / generate the user's TOTP secret.
     *
     * @param id User primary key.
     * @param accountName Account name displayed in the OTP app (usually pass username).
     * @param issuer Issuer name displayed in the OTP app (usually pass the application name).
     * @return Secret + otpauth URL; returns null if the user does not exist or the database write fails.
     */
    @PostMapping("/resetAuthKey")
    fun resetAuthKey(
        @RequestParam id: String,
        @RequestParam accountName: String,
        @RequestParam(required = false, defaultValue = "kudos") issuer: String,
    ): AuthKeySetup? = service.resetAuthKey(id, accountName, issuer)

    /** Clear TOTP secret (disable two-factor verification). */
    @PostMapping("/cleanAuthKey")
    fun cleanAuthKey(@RequestParam id: String): Boolean =
        service.cleanAuthKey(id)

    /** Verify the 6-digit TOTP code provided by the user. */
    @PostMapping("/verifyAuthCode")
    fun verifyAuthCode(@RequestParam id: String, @RequestParam code: Long): Boolean =
        service.verifyAuthCode(id, code)

    /**
     * Freeze the account. Login attempts within the effective window will be rejected with ACCOUNT_FROZEN.
     *
     * @param id User primary key.
     * @param freezeType Freeze type dictionary code (manual / auto / admin / scheduled, etc.).
     * @param freezeTitle Short title (nullable).
     * @param freezeContent Detailed description (nullable).
     * @param freezeStartTime ISO-8601 datetime; omit = take effect immediately.
     * @param freezeEndTime   ISO-8601 datetime; omit = permanent freeze.
     */
    @PostMapping("/freezeAccount")
    fun freezeAccount(
        @RequestParam id: String,
        @RequestParam freezeType: String,
        @RequestParam(required = false) freezeTitle: String?,
        @RequestParam(required = false) freezeContent: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) freezeStartTime: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) freezeEndTime: LocalDateTime?,
    ): Boolean = service.freezeAccount(id, freezeType, freezeTitle, freezeContent, freezeStartTime, freezeEndTime)

    /** Unfreeze: clear all freeze_* fields. */
    @PostMapping("/unfreezeAccount")
    fun unfreezeAccount(@RequestParam id: String): Boolean =
        service.unfreezeAccount(id)

    /**
     * Manually trigger "clean expired freezes". Normally run automatically by `AutoUnfreezeScheduler` —
     * provided the deployment has enabled `@EnableScheduling`. This endpoint is an emergency / debug channel.
     *
     * @return The number of accounts cleaned up this run.
     */
    @PostMapping("/cleanExpiredFreezes")
    fun cleanExpiredFreezes(): Int =
        service.cleanExpiredFreezes()

}
