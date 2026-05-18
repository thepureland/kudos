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
 * 用户账号管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/user/account")
class UserAccountAdminController :
    BaseCrudController<String, IUserAccountService, UserAccountQuery, UserAccountRow, UserAccountDetail, UserAccountEdit, UserAccountFormCreate, UserAccountFormUpdate>() {

    /** 更新启用状态 */
    @PutMapping("/updateActive")
    fun updateActive(@RequestParam id: String, @RequestParam active: Boolean): Boolean =
        service.updateActive(id, active)

    /** 重置登录密码（管理员操作） */
    @PostMapping("/resetPassword")
    fun resetPassword(@RequestParam id: String, @RequestParam newPassword: String): Boolean =
        service.resetPassword(id, newPassword)

    /** 重置安全密码（管理员操作） */
    @PostMapping("/resetSecurityPassword")
    fun resetSecurityPassword(@RequestParam id: String, @RequestParam newPassword: String): Boolean =
        service.resetSecurityPassword(id, newPassword)

    /**
     * 重置/生成用户的 TOTP secret。
     *
     * @param id 用户主键
     * @param accountName OTP App 里显示的账号名（一般传 username）
     * @param issuer OTP App 里显示的发行方名（一般传应用名）
     * @return secret + otpauth URL；用户不存在或写库失败返回 null
     */
    @PostMapping("/resetAuthKey")
    fun resetAuthKey(
        @RequestParam id: String,
        @RequestParam accountName: String,
        @RequestParam(required = false, defaultValue = "kudos") issuer: String,
    ): AuthKeySetup? = service.resetAuthKey(id, accountName, issuer)

    /** 清除 TOTP secret（关闭二次验证） */
    @PostMapping("/cleanAuthKey")
    fun cleanAuthKey(@RequestParam id: String): Boolean =
        service.cleanAuthKey(id)

    /** 校验用户提供的 6 位 TOTP 验证码 */
    @PostMapping("/verifyAuthCode")
    fun verifyAuthCode(@RequestParam id: String, @RequestParam code: Long): Boolean =
        service.verifyAuthCode(id, code)

    /**
     * 冻结账号。在生效窗口内的登录尝试会被拒绝返回 ACCOUNT_FROZEN。
     *
     * @param id 用户主键
     * @param freezeType 冻结类型字典码（manual / auto / admin / scheduled 等）
     * @param freezeTitle 简短标题（可空）
     * @param freezeContent 详细说明（可空）
     * @param freezeStartTime ISO-8601 日期时间；不传 = 立即生效
     * @param freezeEndTime   ISO-8601 日期时间；不传 = 永久冻结
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

    /** 解除冻结：清空全部 freeze_* 字段。 */
    @PostMapping("/unfreezeAccount")
    fun unfreezeAccount(@RequestParam id: String): Boolean =
        service.unfreezeAccount(id)

    /**
     * 手动触发"清理已过期冻结"。通常由 `AutoUnfreezeScheduler` 自动跑——前提是部署方
     * 启用了 `@EnableScheduling`。此端点为应急/调试通道。
     *
     * @return 本次清理掉的账号数
     */
    @PostMapping("/cleanExpiredFreezes")
    fun cleanExpiredFreezes(): Int =
        service.cleanExpiredFreezes()

}
