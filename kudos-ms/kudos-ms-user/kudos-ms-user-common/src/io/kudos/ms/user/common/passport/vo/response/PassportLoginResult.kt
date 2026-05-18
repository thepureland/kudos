package io.kudos.ms.user.common.passport.vo.response

import io.kudos.ms.user.common.passport.enums.PassportLoginStatusEnum
import java.io.Serializable


/**
 * 登录结果。所有结局（成功 / 用户不存在 / 密码错 / 禁用）都通过本类回传，
 * HTTP 层一律 200，由 [status] 区分。
 *
 * @author K
 * @since 1.0.0
 */
data class PassportLoginResult(

    /** 登录结果状态 */
    val status: PassportLoginStatusEnum,

    /** 用户信息，仅 [status]=SUCCESS 时有值 */
    val userInfo: UserInfoModel? = null,

    /** 累计登录错误次数（含本次失败），仅 [status]=WRONG_PASSWORD 时有值 */
    val loginErrorTimes: Int? = null,

    /** 附加错误说明，可为 null */
    val message: String? = null,

) : Serializable {

    companion object {
        private const val serialVersionUID = 1L

        fun success(userInfo: UserInfoModel): PassportLoginResult =
            PassportLoginResult(status = PassportLoginStatusEnum.SUCCESS, userInfo = userInfo)

        fun userNotFound(): PassportLoginResult =
            PassportLoginResult(status = PassportLoginStatusEnum.USER_NOT_FOUND, message = "用户不存在")

        fun wrongPassword(loginErrorTimes: Int): PassportLoginResult =
            PassportLoginResult(
                status = PassportLoginStatusEnum.WRONG_PASSWORD,
                loginErrorTimes = loginErrorTimes,
                message = "密码错误"
            )

        fun inactive(): PassportLoginResult =
            PassportLoginResult(status = PassportLoginStatusEnum.INACTIVE, message = "账号已禁用")
    }
}
